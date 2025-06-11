import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.View.ScalaBoidsView

import scala.concurrent.duration.*

object BoidsSimulation {
  // Message protocol
  sealed trait Command

  case class BoidUpdated(boidRef: ActorRef[BoidActor.Command]) extends Command

  // Configuration parameters
  private val SimWidth = 800
  private val SimHeight = 800
  private val NumBoids = 10000
  private val TickInterval = 40.millis

  def apply(): Behavior[Command] = Behaviors.withTimers {
    def sendMessages(viewActorRef: ActorRef[BoidActor.Command], boids: IndexedSeq[ActorRef[BoidActor.Command]]): Unit = {
      boids.foreach(_ ! BoidActor.VelocityTick)
      boids.foreach(_ ! BoidActor.PositionTick)
      viewActorRef ! BoidActor.ViewTick
    }

    timers =>
      Behaviors.setup { context =>
        // Create the view
        val view = new ScalaBoidsView(SimWidth, SimHeight)

        // Create actors
        val viewActorRef = context.spawn(ViewActor(view), "viewActor")
        val spacePartitionerRef = context.spawn(SpacePartitionerActor(), "spacePartitioner")

        val boids = (1 to NumBoids).map { i =>
          context.spawn(BoidActor(viewActorRef, spacePartitionerRef, context.self), s"boid-$i")
        }

        sendMessages(viewActorRef, boids)
        var counter = 0
        var lastFrameTime = 0L

        Behaviors.receiveMessage {
          case BoidUpdated(boidRef) =>
            counter += 1
            if (counter == boids.size) {
              while (System.currentTimeMillis() - lastFrameTime < TickInterval.toMillis) {
                // Wait for the tick interval to pass
              }
              counter = 0
              sendMessages(viewActorRef, boids)
              val elapsedTimeToFps = System.currentTimeMillis() - lastFrameTime
              val fps = (1000.0 / elapsedTimeToFps).toInt
              view.update(fps)
              lastFrameTime = System.currentTimeMillis()
            }
            Behaviors.same
        }
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