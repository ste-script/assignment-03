package pcd.ass03

import akka.actor.typed.scaladsl.{Behaviors, Routers, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import pcd.ass01.View.ScalaBoidsView

import scala.concurrent.duration.*

object BoidsSimulation:
  // Message protocol
  sealed trait Command

  case object ResumeSimulation extends Command
  private case object Tick extends Command // Keep original name for simplicity

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
  private val TickTimerKey = "TickTimer"

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      var counterPosition = 0
      var counterVelocity = 0
      var lastFrameTime = 0L
      var pendingTermination = false
      val view = new ScalaBoidsView(SimWidth, SimHeight)
      var boids: Seq[ActorRef[BoidActor.Command]] = Seq.empty

      // Create the view
      view.setActorRef(context.self)

      // Create actors
      val viewActorRef = context.spawn(ViewActor(view), "viewActor")
      val spacePartitionerPool = Routers.pool(4) {
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

      def startTick(): Unit =
        timers.startTimerAtFixedRate(TickTimerKey, Tick, TickInterval)

      def stopTick(): Unit =
        timers.cancel(TickTimerKey)

      def resetCounters(): Unit =
        counterPosition = 0
        counterVelocity = 0

      def pauseState: Behavior[Command] = Behaviors.receiveMessage {
        case ResumeSimulation =>
          resetCounters()
          lastFrameTime = System.currentTimeMillis()
          startTick()
          runningState
        case TerminateSimulation =>
          boids.foreach(_ ! BoidActor.Terminate)
          Behaviors.stopped
        case _ => Behaviors.same
      }

      def terminatedState: Behavior[Command] = Behaviors.receiveMessage {
        case StartSimulation =>
          boids = (1 to NumBoids).map { i =>
            context.spawn(
              BoidActor(viewActorRef, spacePartitionerBroadcast, context.self),
              s"boid-$i"
            )
          }
          viewActorRef ! ViewActor.SimulationStarted
          resetCounters()
          pendingTermination = false
          lastFrameTime = System.currentTimeMillis()
          startTick()
          runningState
        case _ => Behaviors.same
      }

      def runningState: Behavior[Command] = Behaviors.receiveMessage {
        case PauseSimulation =>
          stopTick()
          pauseState

        case TerminateSimulation =>
          pendingTermination = true
          Behaviors.same

        case Tick =>
          // Trigger velocity updates for all boids
          boids.foreach(_ ! BoidActor.VelocityTick)
          Behaviors.same

        case BoidVelocityUpdated(_) =>
          counterVelocity += 1
          if counterVelocity == boids.size then
            // All velocities updated, start position updates
            boids.foreach(_ ! BoidActor.PositionTick)
            counterVelocity = 0
          Behaviors.same

        case BoidPositionUpdated(_) =>
          counterPosition += 1
          if counterPosition == boids.size then
            // All positions updated
            if pendingTermination then
              stopTick()
              boids.foreach(_ ! BoidActor.Terminate)
              viewActorRef ! ViewActor.SimulationStopped
              terminatedState
            else
              // Complete the frame

              // Calculate and update FPS
              val currentTime = System.currentTimeMillis()
              val elapsedTime = currentTime - lastFrameTime
              val fps = if elapsedTime > 0 then (1000.0 / elapsedTime).toInt else 0
              lastFrameTime = currentTime
              viewActorRef ! ViewActor.ViewTick(fps)
              // Reset counters for next frame
              resetCounters()

              // Reset counter for next frame
              counterPosition = 0
              Behaviors.same
          else Behaviors.same

        case _ => Behaviors.same
      }

      terminatedState
    }
  }

@main
def main(): Unit =
  val system = ActorSystem(BoidsSimulation(), "BoidSystem")
  system ! BoidsSimulation.StartSimulation

  sys.addShutdownHook {
    system.terminate()
  }
