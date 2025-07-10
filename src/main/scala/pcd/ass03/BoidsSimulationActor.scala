package pcd.ass03

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import pcd.ass03.View.BoidsView

import scala.concurrent.duration.*

object BoidsSimulationActor:
  // Message protocol
  sealed trait Command

  private case object SimulationTick extends Command

  case object StartSimulation extends Command
  case object ResumeSimulation extends Command
  case object PauseSimulation extends Command
  case object TerminateSimulation extends Command

  case class UpdateNumberOfBoids(numBoids: Int) extends Command
  case class UpdateCohesionWeight(weight: Double) extends Command
  case class UpdateSeparationWeight(weight: Double) extends Command
  case class UpdateAlignmentWeight(weight: Double) extends Command

  case class BoidVelocityUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command
  case class BoidPositionUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  // Configuration parameters
  private object Config:
    val simWidth: Int = 800
    val simHeight: Int = 800
    val startNumBoids: Int = 1500
    val tickInterval: FiniteDuration = 40.millis
    private val availableCores: Int = Runtime.getRuntime.availableProcessors()
    val spacePartitionerPoolSize: Int = 4

  // Simulation state
  private case class SimulationState(
      counterPosition: Int = 0,
      counterVelocity: Int = 0,
      lastFrameTime: Long = 0L,
      goToTerminatedState: Boolean = false,
      boids: Seq[ActorRef[BoidActor.Command]] = Seq.empty,
      tickCounter: Long = 0L,
      lastTickValue: Long = 0L,
      targetNumberOfBoids: Int = Config.startNumBoids,
      cohesionWeight: Double = 1.0,
      separationWeight: Double = 1.0,
      alignmentWeight: Double = 1.0
  ):
    def resetCounters: SimulationState = copy(
      counterPosition = 0,
      counterVelocity = 0,
      lastFrameTime = System.currentTimeMillis()
    )

    def incrementVelocityCounter: SimulationState = copy(counterVelocity = counterVelocity + 1)
    def incrementPositionCounter: SimulationState = copy(counterPosition = counterPosition + 1)
    def incrementTickCounter: SimulationState = copy(tickCounter = tickCounter + 1)
    def resetPositionCounter: SimulationState = copy(counterPosition = 0)
    def resetVelocityCounter: SimulationState = copy(counterVelocity = 0)
    def setBoids(newBoids: Seq[ActorRef[BoidActor.Command]]): SimulationState = copy(boids = newBoids)
    def setTerminationFlag(flag: Boolean): SimulationState = copy(goToTerminatedState = flag)
    def withUpdatedLastTickValue: SimulationState = copy(lastTickValue = tickCounter)
    def updateNumberOfBoids(newCount: Int): SimulationState =
      copy(targetNumberOfBoids = newCount)
    def updateCohesionWeight(weight: Double): SimulationState =
      copy(cohesionWeight = weight)
    def updateSeparationWeight(weight: Double): SimulationState =
      copy(separationWeight = weight)
    def updateAlignmentWeight(weight: Double): SimulationState =
      copy(alignmentWeight = weight)

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      import Config.*

      // Timer management
      def startTick(): Unit = timers.startTimerAtFixedRate("tick", SimulationTick, tickInterval)
      def cancelTick(): Unit = timers.cancel("tick")

      // Actor creation helpers
      def createView(): BoidsView =
        val view = new BoidsView(simWidth, simHeight)
        view.setActorRef(context.self)
        view

      def createSpacePartitionerBroadcast(): ActorRef[SpacePartitionerActor.Command] =
        val spacePartitionerPool = Routers.pool(spacePartitionerPoolSize)(SpacePartitionerActor())
        val conditionPredicate: Any => Boolean = {
          case _: SpacePartitionerActor.FindNeighbors => false
          case _ => true
        }
        context.spawn(
          spacePartitionerPool.withBroadcastPredicate(conditionPredicate),
          "spacePartitionerBroadcast"
        )

      def boidActorMaker(
          viewActor: ActorRef[ViewActor.Command],
          spacePartitioner: ActorRef[SpacePartitionerActor.Command],
          targetNumBoids: Int,
          currentBoids: Seq[ActorRef[BoidActor.Command]],
          cohesionWeight: Double,
          separationWeight: Double,
          alignmentWeight: Double
      ): Seq[ActorRef[BoidActor.Command]] =
        if currentBoids.size >= targetNumBoids then
          context.log.info(s"Current boids count: ${currentBoids.size}, target: $targetNumBoids")
          val excessBoids = currentBoids.size - targetNumBoids
          if excessBoids > 0 then
            context.log.info(s"Removing excess boids: $excessBoids")
            val boidToStop = currentBoids.takeRight(excessBoids);
            boidToStop.foreach { boid =>
              context.log.info(s"Stopping boid: $boid")
              boid ! BoidActor.Terminate
            }
            currentBoids.dropRight(excessBoids)
          else
            context.log.info("No excess boids, returning current boids.")
            currentBoids
        else
          context.log.info(s"Creating new boids: ${targetNumBoids - currentBoids.size} more needed.")
          val newBoids = (1 to targetNumBoids - currentBoids.size).map { i =>
            context.spawnAnonymous(
              BoidActor(viewActor, spacePartitioner, context.self, cohesionWeight, separationWeight, alignmentWeight)
            )
          }
          newBoids ++ currentBoids

      // Initialize actors
      val viewActorRef = context.spawn(ViewActor(createView()), "viewActor")
      val spacePartitionerBroadcast = createSpacePartitionerBroadcast()

      // Behavior implementations

      def alwaysValidBehavior(
          state: SimulationState
      ): (SimulationState => Behavior[Command]) => PartialFunction[Command, Behavior[Command]] =
        (nextBehavior: SimulationState => Behavior[Command]) => { case UpdateNumberOfBoids(newBoidsCount) =>
          nextBehavior(state.updateNumberOfBoids(newBoidsCount))
        }

      def pausedBehavior(state: SimulationState): Behavior[Command] =
        Behaviors.receiveMessage(
          alwaysValidBehavior(state)(pausedBehavior).orElse {
            case ResumeSimulation =>
              startTick()
              val resetState = state.resetCounters
              resetState.boids.foreach(_ ! BoidActor.VelocityTick)
              runningBehavior(resetState)

            case TerminateSimulation =>
              state.boids.foreach(_ ! BoidActor.Terminate)
              Behaviors.stopped

            case _ => Behaviors.same
          }
        )

      def terminatedBehavior(state: SimulationState): Behavior[Command] =
        Behaviors.receiveMessage {
          alwaysValidBehavior(state)(terminatedBehavior).orElse {
            case StartSimulation =>
              startTick()
              val newBoids =
                boidActorMaker(
                  viewActorRef,
                  spacePartitionerBroadcast,
                  state.targetNumberOfBoids,
                  state.boids,
                  state.cohesionWeight,
                  state.separationWeight,
                  state.alignmentWeight
                )
              val newState = state.setBoids(newBoids).resetCounters
              viewActorRef ! ViewActor.SimulationStarted
              newBoids.foreach(_ ! BoidActor.VelocityTick)
              runningBehavior(newState)
            case _ => Behaviors.same
          }
        }

      def runningBehavior(state: SimulationState): Behavior[Command] =
        Behaviors.receiveMessage {
          alwaysValidBehavior(state)(runningBehavior).orElse {
            case PauseSimulation =>
              cancelTick()
              pausedBehavior(state)

            case TerminateSimulation =>
              runningBehavior(state.setTerminationFlag(true))

            case BoidVelocityUpdated(_) =>
              val newState = state.incrementVelocityCounter
              if newState.counterVelocity == state.boids.size then
                context.log.debug("All boids updated their velocity.")
                state.boids.foreach(_ ! BoidActor.PositionTick)
                runningBehavior(newState.resetVelocityCounter)
              else runningBehavior(newState)

            case BoidPositionUpdated(_) =>
              val newState = state.incrementPositionCounter
              def allBoidUpdatedPosition: Boolean =
                newState.counterPosition == state.boids.size
              def needToChangeBoidNumber: Boolean =
                newState.targetNumberOfBoids != state.boids.size

              if allBoidUpdatedPosition then
                if state.goToTerminatedState then
                  cancelTick()
                  state.boids.foreach(_ ! BoidActor.Terminate)
                  viewActorRef ! ViewActor.SimulationStopped
                  terminatedBehavior(SimulationState().updateNumberOfBoids(state.targetNumberOfBoids))
                else if needToChangeBoidNumber then
                  context.log.info(s"Updating number of boids ${state.boids.size} to ${state.targetNumberOfBoids}.")
                  val newBoids =
                    boidActorMaker(
                      viewActorRef,
                      spacePartitionerBroadcast,
                      state.targetNumberOfBoids,
                      state.boids,
                      state.cohesionWeight,
                      state.separationWeight,
                      state.alignmentWeight
                    )
                  runningBehavior(newState.setBoids(newBoids).incrementTickCounter.resetPositionCounter)
                else
                  context.log.debug("All boids updated their position.")
                  runningBehavior(newState.incrementTickCounter.resetPositionCounter)
              else runningBehavior(newState)

            case UpdateCohesionWeight(weight) =>
              val updatedState = state.updateCohesionWeight(weight)
              context.log.info(s"Updated cohesion weight to $weight")
              state.boids.foreach(_ ! BoidActor.UpdateCohesionWeight(weight))
              runningBehavior(updatedState)
            case UpdateSeparationWeight(weight) =>
              val updatedState = state.updateSeparationWeight(weight)
              context.log.info(s"Updated separation weight to $weight")
              state.boids.foreach(_ ! BoidActor.UpdateSeparationWeight(weight))
              runningBehavior(updatedState)
            case UpdateAlignmentWeight(weight) =>
              val updatedState = state.updateAlignmentWeight(weight)
              updatedState.boids.foreach(_ ! BoidActor.UpdateAlignmentWeight(weight))
              runningBehavior(updatedState)
            case SimulationTick =>
              if state.counterPosition == 0 && state.tickCounter > state.lastTickValue then
                context.log.debug("Simulation tick received, and all boids have updated their position")
                state.boids.foreach(_ ! BoidActor.VelocityTick)

                val elapsedTime = System.currentTimeMillis() - state.lastFrameTime
                val fps = if elapsedTime > 0 then (1000.0 / elapsedTime).toInt else 0
                val updatedState = state.copy(lastFrameTime = System.currentTimeMillis()).withUpdatedLastTickValue

                viewActorRef ! ViewActor.ViewTick(fps)
                runningBehavior(updatedState)
              else
                context.log.debug("Simulation tick received, missing boid position updates")
                Behaviors.same

            case _ => Behaviors.same
          }
        }

      terminatedBehavior(SimulationState())
    }
  }

@main
def main(): Unit =
  val system = ActorSystem(BoidsSimulationActor(), "BoidSystem")
  system ! BoidsSimulationActor.StartSimulation

  // Graceful shutdown on Ctrl-C
  sys.addShutdownHook {
    system.terminate()
  }
