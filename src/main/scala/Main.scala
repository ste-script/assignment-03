import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
// "Actor" module definition


def rootBehavior: Behavior[Unit] =
  Behaviors.setup { context =>
    val boid1 = context.spawn(BoidActor(), "boid1")
    val boidsList = for (i <- 1 to 5) yield context.spawnAnonymous(BoidActor())
    Behaviors.empty
  }

object HelloWorldAkkaTyped extends App:
  private val system = ActorSystem(rootBehavior, "BoidSystem")
  Thread.sleep(5000)
  system.terminate()