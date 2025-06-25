package pcd.ass03

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import pcd.ass01.View.ScalaBoidsView

import scala.concurrent.duration.*

object BoidsSimulation {
  // Message protocol
  sealed trait Command

  case object ResumeSimulation extends Command

  case object PauseSimulation extends Command
  case object TerminateSimulation extends Command

  case class BoidVelocityUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  case class BoidPositionUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  // Configuration parameters
  private val SimWidth = 800
  private val SimHeight = 800
  private val NumBoids = 2000
  private val TickInterval = 40.millis


  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var counterPosition = 0
    var counterVelocity = 0
    var lastFrameTime = 0L
    val view = new ScalaBoidsView(SimWidth, SimHeight)
    var boids: Seq[ActorRef[BoidActor.Command]] = Seq.empty
    // Create the view
    view.setActorRef(context.self)

    // Create actors
    val viewActorRef = context.spawn(ViewActor(view), "viewActor")
    val availableCores = Runtime.getRuntime.availableProcessors() + 1
    val spacePartitionerPool = Routers.pool(availableCores) {
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
    boids = (1 to NumBoids).map { i =>
      context.spawn(
        BoidActor(viewActorRef, spacePartitionerBroadcast, context.self),
        s"boid-$i",
        DispatcherSelector.fromConfig("boids-dispatcher")
      )
    }

    boids.foreach(_ ! BoidActor.VelocityTick)

    def pauseState: Behavior[Command] = Behaviors.receiveMessage {
      case ResumeSimulation =>
        // Reset counters and last frame time
        counterPosition = 0
        counterVelocity = 0
        lastFrameTime = System.currentTimeMillis()
        boids.foreach(_ ! BoidActor.VelocityTick)
        runningState
      case _ => Behaviors.same // Ignore other messages
    }

    def runningState: Behavior[Command] = Behaviors.receiveMessage {
      case PauseSimulation => pauseState // Transition to pause state
      case TerminateSimulation =>
        // Terminate all boids and the view actor
        boids.foreach(_ ! BoidActor.Terminate)
        Behaviors.stopped
      case BoidVelocityUpdated(boidRef) =>
        counterVelocity += 1
        if (counterVelocity == boids.size) {
          boids.foreach(_ ! BoidActor.PositionTick)
          counterVelocity = 0
        }
        Behaviors.same
      case BoidPositionUpdated(boidRef) =>
        counterPosition += 1
        if (counterPosition == boids.size) {
          while (System.currentTimeMillis() - lastFrameTime < TickInterval.toMillis) {
            // Wait for the tick interval to pass
          }
          viewActorRef ! BoidActor.ViewTick
          boids.foreach(_ ! BoidActor.VelocityTick)
          counterPosition = 0
          val elapsedTimeToFps = System.currentTimeMillis() - lastFrameTime
          val fps = (1000.0 / elapsedTimeToFps).toInt
          view.update(fps)
          lastFrameTime = System.currentTimeMillis()
        }
        Behaviors.same
      case ResumeSimulation => Behaviors.same
    }

    runningState

  }
}


@main
def main(): Unit = {
  val system = ActorSystem(BoidsSimulation(), "BoidSystem")

  // Exit on Ctrl-C
  sys.addShutdownHook {
    system.terminate()
  }
}