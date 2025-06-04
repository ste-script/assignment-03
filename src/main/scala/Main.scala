import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.actor.typed.scaladsl.Behaviors
import BoidMessages._
// "Actor" module definition


def rootBehavior: Behavior[Unit] =
  Behaviors.setup { context =>
    val boid1 = context.spawn(BoidActor(), "boid1")
    val boidsList = for (i <- 1 to 5) yield context.spawnAnonymous(BoidActor())
    boid1 ! UpdatePosition((1.0, 2.0))
    boid1 ! UpdateVelocity((0.5, 0.5))
    boidsList.foreach { boid =>
      boid ! UpdatePosition((scala.util.Random.nextDouble(), scala.util.Random.nextDouble()))
      boid ! UpdateVelocity((scala.util.Random.nextDouble(), scala.util.Random.nextDouble()))
    }
    Behaviors.empty
  }

object HelloWorldAkkaTyped extends App:
  private val system = ActorSystem(rootBehavior, "BoidSystem")
  Thread.sleep(5000)
  system.terminate()