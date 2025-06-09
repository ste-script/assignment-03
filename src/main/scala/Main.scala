import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

def rootBehavior: Behavior[Unit] =
  Behaviors.setup { context =>
    val view = pcd.ass01.View.ScalaBoidsView(800, 800)
    val boidsList = for (i <- 1 to 100) yield context.spawnAnonymous(BoidActor(view))
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    context.system.scheduler.scheduleAtFixedRate(
      initialDelay = scala.concurrent.duration.Duration.Zero,
      interval = scala.concurrent.duration.FiniteDuration(20, "millis") // Adjust the interval as needed
    )(
      () => {
        boidsList.foreach(_ ! BoidActor.VelocityTick)
        boidsList.foreach(_ ! BoidActor.PositionTick)
        view.update(25)
      }
    )
    Behaviors.empty
  }

@main
def main(): Unit =
  val system = ActorSystem(rootBehavior, "BoidSystem")
  //exit on ctrl-c
  sys.addShutdownHook {
    system.terminate()
  }


