package pcd.ass03

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import pcd.ass01.View.ScalaBoidsView

import scala.concurrent.duration.*

object BoidsSimulation:
  // Message protocol
  sealed trait Command

  case object ResumeSimulation extends Command
  private case object SimulationTick extends Command
  case object PauseSimulation extends Command
  case object TerminateSimulation extends Command
  case object StartSimulation extends Command
  case class BoidVelocityUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command
  case class BoidPositionUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  // Configuration parameters
  private object Config:
    val SimWidth: Int = 800
    val SimHeight: Int = 800
    val NumBoids: Int = 2000
    val TickInterval: FiniteDuration = 40.millis
    private val AvailableCores: Int = Runtime.getRuntime.availableProcessors()
    val SpacePartitionerPoolSize: Int = AvailableCores / 4

  // Simulation state
  private case class SimulationState(
    counterPosition: Int = 0,
    counterVelocity: Int = 0,
    lastFrameTime: Long = 0L,
    goToTerminatedState: Boolean = false,
    boids: Seq[ActorRef[BoidActor.Command]] = Seq.empty,
    tickCounter: Long = 0L,
    lastTickValue: Long = 0L
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

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      import Config.*

      // Timer management
      def startTick(): Unit = timers.startTimerAtFixedRate("tick", SimulationTick, TickInterval)
      def cancelTick(): Unit = timers.cancel("tick")

      // Actor creation helpers
      def createView(): ScalaBoidsView =
        val view = new ScalaBoidsView(SimWidth, SimHeight)
        view.setActorRef(context.self)
        view

      def createSpacePartitionerBroadcast(): ActorRef[SpacePartitionerActor.Command] =
        val spacePartitionerPool = Routers.pool(SpacePartitionerPoolSize)(SpacePartitionerActor())
        val conditionPredicate: Any => Boolean = {
          case _: SpacePartitionerActor.FindNeighbors => false
          case _ => true
        }
        context.spawn(
          spacePartitionerPool.withBroadcastPredicate(conditionPredicate),
          "spacePartitionerBroadcast"
        )

      def createBoids(viewActor: ActorRef[ViewActor.Command], 
                     spacePartitioner: ActorRef[SpacePartitionerActor.Command]): Seq[ActorRef[BoidActor.Command]] =
        (1 to NumBoids).map { i =>
          context.spawn(
            BoidActor(viewActor, spacePartitioner, context.self),
            s"boid-$i"
          )
        }

      // Initialize actors
      val viewActorRef = context.spawn(ViewActor(createView()), "viewActor")
      val spacePartitionerBroadcast = createSpacePartitionerBroadcast()

      // Behavior implementations
      def pausedBehavior(state: SimulationState): Behavior[Command] = 
        Behaviors.receiveMessage {
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

      def terminatedBehavior(state: SimulationState): Behavior[Command] = 
        Behaviors.receiveMessage {
          case StartSimulation =>
            startTick()
            val newBoids = createBoids(viewActorRef, spacePartitionerBroadcast)
            val newState = state.setBoids(newBoids).resetCounters
            
            viewActorRef ! ViewActor.SimulationStarted
            newBoids.foreach(_ ! BoidActor.VelocityTick)
            runningBehavior(newState)
          
          case _ => Behaviors.same
        }

      def runningBehavior(state: SimulationState): Behavior[Command] = 
        Behaviors.receiveMessage {
          case PauseSimulation =>
            cancelTick()
            pausedBehavior(state)
          
          case TerminateSimulation =>
            cancelTick()
            runningBehavior(state.setTerminationFlag(true))
          
          case BoidVelocityUpdated(_) =>
            val newState = state.incrementVelocityCounter
            if newState.counterVelocity == state.boids.size then
              context.log.debug("All boids updated their velocity.")
              state.boids.foreach(_ ! BoidActor.PositionTick)
              runningBehavior(newState.resetVelocityCounter)
            else
              runningBehavior(newState)
          
          case BoidPositionUpdated(_) =>
            val newState = state.incrementPositionCounter
            
            if newState.counterPosition == state.boids.size && state.goToTerminatedState then
              state.boids.foreach(_ ! BoidActor.Terminate)
              viewActorRef ! ViewActor.SimulationStopped
              terminatedBehavior(SimulationState())
            else if newState.counterPosition == state.boids.size then
              context.log.debug("All boids updated their position.")
              runningBehavior(newState.incrementTickCounter.resetPositionCounter)
            else
              runningBehavior(newState)
          
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

      terminatedBehavior(SimulationState())
    }
  }

@main
def main(): Unit =
  val system = ActorSystem(BoidsSimulation(), "BoidSystem")
  system ! BoidsSimulation.StartSimulation
  
  // Graceful shutdown on Ctrl-C
  sys.addShutdownHook {
    system.terminate()
  }
