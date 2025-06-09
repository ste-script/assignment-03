import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

def rootBehavior: Behavior[Unit] =
  Behaviors.setup { context =>
    val view = pcd.ass01.View.ScalaBoidsView(800, 800)
    // Props for the custom dispatcher
    val boidDispatcher = DispatcherSelector.fromConfig("boid-dispatcher")
    val boidsList = for (i <- 1 to 500) yield context.spawnAnonymous(BoidActor(view), DispatcherSelector.fromConfig("boid-dispatcher"))
    implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    //wait for the boids to be created
    //sleep for a short duration to ensure all actors are initialized
    context.system.scheduler.scheduleAtFixedRate(
      initialDelay = scala.concurrent.duration.Duration(5, "seconds"),
      interval = scala.concurrent.duration.FiniteDuration(40, "millis") // Adjust the interval as needed
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


