import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
// "Actor" module definition


def rootBehavior: Behavior[Unit] =
  Behaviors.setup { context =>
    val view = pcd.ass01.View.ScalaBoidsView(800,800)
    val boid1 = context.spawn(BoidActor(view), "boid1")
    val boidsList = for (i <- 1 to 500) yield context.spawnAnonymous(BoidActor(view))
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    context.system.scheduler.scheduleAtFixedRate(
      initialDelay = scala.concurrent.duration.Duration.Zero,
      interval = scala.concurrent.duration.FiniteDuration(20, "millis") // Adjust the interval as needed
    )(
      new Runnable {
        def run(): Unit = {
          boidsList.foreach(_ ! BoidActor.VelocityTick)
          boidsList.foreach(_ ! BoidActor.PositionTick)
          view.update(25)
        }
      }
    )
    Behaviors.empty
  }

object HelloWorldAkkaTyped extends App:
  private val system = ActorSystem(rootBehavior, "BoidSystem")
  Thread.sleep(5000)
  system.terminate()


