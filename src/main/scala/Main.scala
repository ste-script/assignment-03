import akka.actor.typed.{ActorRef, ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import pcd.ass01.View.ScalaBoidsView

import scala.concurrent.duration.*

object BoidsSimulation {
  // Message protocol
  sealed trait Command

  case class BoidVelocityUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  case class BoidPositionUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  // Configuration parameters
  private val SimWidth = 800
  private val SimHeight = 800
  private val NumBoids = 2000
  private val TickInterval = 40.millis

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    // Create the view
    val view = new ScalaBoidsView(SimWidth, SimHeight)

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
    val akkaDispatcher = context.system.settings.config.getConfig("akka.actor.default-dispatcher")
    val boids = (1 to NumBoids).map { i =>
      context.spawn(
        BoidActor(viewActorRef, spacePartitionerBroadcast, context.self),
        s"boid-$i",
        DispatcherSelector.fromConfig("boids-dispatcher")
      )
    }

    boids.foreach(_ ! BoidActor.VelocityTick)
    var counterPosition = 0
    var counterVelocity = 0
    var lastFrameTime = 0L

    Behaviors.receiveMessage {
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
    }
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