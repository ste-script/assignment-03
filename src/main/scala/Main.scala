import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.actor.typed.scaladsl.Behaviors
import BoidMessages._

// "Actor" module definition
object BoidActor:

  // Define the state of a boid
  final case class BoidState(position: (Double, Double), velocity: (Double, Double))

  // The behavior of the Boid actor
  def apply(): Behavior[BoidMessage] = Behaviors.setup { context =>
    var position = (0.0, 0.0)
    var velocity = (0.0, 0.0)
    val boidId = context.self.path.name

    Behaviors.receiveMessage {
      case UpdatePosition(newPosition) =>
        context.log.info(s"Updating position of boid $boidId to $newPosition")
        position = newPosition
        Behaviors.same

      case UpdateVelocity(newVelocity) =>
        context.log.info(s"Updating velocity of boid $boidId to $newVelocity")
        velocity = newVelocity
        Behaviors.same

      case GetState() =>
        context.log.info(s"Current state of boid $boidId: position = $position, velocity = $velocity")
        Behaviors.same
    }
  }

def rootBehavior: Behavior[Unit] =
  Behaviors.setup { context =>
    val boid1 = context.spawn(BoidActor(), "boid1")
    val boidsList = for (i <- 1 to 5) yield context.spawnAnonymous(BoidActor())
    boid1 ! UpdatePosition((1.0, 2.0))
    boid1 ! UpdateVelocity((0.5, 0.5))
    boid1 ! GetState()
    boidsList.foreach { boid =>
      boid ! UpdatePosition((scala.util.Random.nextDouble(), scala.util.Random.nextDouble()))
      boid ! UpdateVelocity((scala.util.Random.nextDouble(), scala.util.Random.nextDouble()))
      boid ! GetState()
    }
    Behaviors.empty
  }

object HelloWorldAkkaTyped extends App:
  private val system = ActorSystem(rootBehavior, "BoidSystem")
  Thread.sleep(5000)
  system.terminate()