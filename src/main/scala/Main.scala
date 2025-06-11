import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._

object BoidsSimulation {
  // Message protocol
  sealed trait Command

  private case object Tick extends Command

  // Configuration parameters
  private val SimWidth = 800
  private val SimHeight = 800
  private val NumBoids = 500
  private val TickInterval = 40.millis
  private val FrameRate = 25

  def apply(): Behavior[Command] = Behaviors.withTimers { timers =>
    Behaviors.setup { context =>
      // Create the view
      val view = new pcd.ass01.View.ScalaBoidsView(SimWidth, SimHeight)

      // Create actors
      val viewActorRef = context.spawn(ViewActor(view), "viewActor")
      val spacePartitionerRef = context.spawn(SpacePartitionerActor(), "spacePartitioner")

      val boids = (1 to NumBoids).map { i =>
        context.spawn(BoidActor(viewActorRef, spacePartitionerRef), s"boid-$i")
      }

      // Start periodic timer
      timers.startTimerWithFixedDelay(Tick, Tick, TickInterval)

      // Return behavior for handling ticks
      Behaviors.receiveMessage {
        case Tick =>
          // Update velocities then positions
          boids.foreach(_ ! BoidActor.VelocityTick)
          boids.foreach(_ ! BoidActor.PositionTick)

          // Update view
          viewActorRef ! BoidActor.ViewTick
          view.update(FrameRate)

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