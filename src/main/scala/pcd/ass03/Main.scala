package pcd.ass03

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
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
  private val SimWidth = 800
  private val SimHeight = 800
  private val NumBoids = 2000
  private val TickInterval = 40.millis

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      var counterPosition = 0
      var counterVelocity = 0
      var lastFrameTime = 0L
      var goToTerminatedState = false
      var boids: Seq[ActorRef[BoidActor.Command]] = Seq.empty
      var tickCounter = 0L
      var lastTickValue = 0L

      def startTick(): Unit =
        timers.startTimerAtFixedRate("tick", SimulationTick, TickInterval)

      def cancelTick(): Unit =
        timers.cancel("tick")

      // Create the view
      def spawnView =
        val view = new ScalaBoidsView(SimWidth, SimHeight)
        view.setActorRef(context.self)
        view

      // Create actors
      val viewActorRef = context.spawn(ViewActor(spawnView), "viewActor")
      val availableCores = Runtime.getRuntime.availableProcessors()
      val spacePartitionerPool = Routers.pool(availableCores / 4) {
        SpacePartitionerActor()
      }

      def conditionPredicate[T]: T => Boolean = {
        case _: SpacePartitionerActor.FindNeighbors => false
        case _ => true
      }

      val spacePartitionerBroadcast = context.spawn(
        spacePartitionerPool.withBroadcastPredicate(conditionPredicate),
        "spacePartitionerBroadcast"
      )

      def pauseState: Behavior[Command] = Behaviors.receiveMessage {
        case ResumeSimulation =>
          startTick()
          // Reset counters and last frame time
          counterPosition = 0
          counterVelocity = 0
          lastFrameTime = System.currentTimeMillis()
          boids.foreach(_ ! BoidActor.VelocityTick)
          runningState
        case TerminateSimulation =>
          // Terminate all boids and the view actor
          boids.foreach(_ ! BoidActor.Terminate)
          Behaviors.stopped
        case _ => Behaviors.same // Ignore other messages
      }

      def terminatedState: Behavior[Command] = Behaviors.receiveMessage {
        case StartSimulation =>
          startTick()
          // Reset counters and last frame time
          boids = (1 to NumBoids).map { i =>
            context.spawn(
              BoidActor(viewActorRef, spacePartitionerBroadcast, context.self),
              s"boid-$i"
            )
          }
          viewActorRef ! ViewActor.SimulationStarted
          boids.foreach(_ ! BoidActor.VelocityTick)
          counterPosition = 0
          counterVelocity = 0
          runningState // Transition to running state
        case _ => Behaviors.same // Ignore other messages
      }

      def runningState: Behavior[Command] = Behaviors.receiveMessage {
        case PauseSimulation =>
          cancelTick()
          pauseState
        case TerminateSimulation =>
          cancelTick()
          goToTerminatedState = true
          Behaviors.same
        case BoidVelocityUpdated(boidRef) =>
          counterVelocity += 1
          if counterVelocity == boids.size then
            context.log.debug("All boids updated their velocity.")
            boids.foreach(_ ! BoidActor.PositionTick)
            counterVelocity = 0
          Behaviors.same
        case BoidPositionUpdated(boidRef) =>
          counterPosition += 1
          if counterPosition == boids.size && goToTerminatedState then
            goToTerminatedState = false
            boids.foreach(_ ! BoidActor.Terminate)
            viewActorRef ! ViewActor.SimulationStopped
            counterPosition = 0
            terminatedState
          else if counterPosition == boids.size then
            context.log.debug("All boids updated their position.")
            tickCounter += 1
            counterPosition = 0
            Behaviors.same
          else Behaviors.same
        case SimulationTick =>
          if counterPosition == 0 && tickCounter > lastTickValue then
            context.log.debug("Simulation tick received, and all boids have updated their position")
            boids.foreach(_ ! BoidActor.VelocityTick)
            lastTickValue = tickCounter
            val elapsedTimeToFps = System.currentTimeMillis() - lastFrameTime
            val fps = (1000.0 / elapsedTimeToFps).toInt
            lastFrameTime = System.currentTimeMillis()
            viewActorRef ! ViewActor.ViewTick(fps)
          else context.log.debug("Simulation tick received, missing boid position updates")
          Behaviors.same
        case _ => Behaviors.same
      }

      terminatedState
    }
  }
@main
def main(): Unit =
  val system = ActorSystem(BoidsSimulation(), "BoidSystem")
  system ! BoidsSimulation.StartSimulation
  // Exit on Ctrl-C
  sys.addShutdownHook {
    system.terminate()
  }
