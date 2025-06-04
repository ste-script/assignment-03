import BoidMessages._
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object BoidActor:

  // Define the state of a boid

  // The behavior of the Boid actor
  def apply(): Behavior[BoidMessage] = Behaviors.setup: context =>
    var position = (0.0, 0.0)
    var velocity = (0.0, 0.0)
    val boidId = context.self.path.name

    Behaviors.receiveMessage:
      case UpdatePosition(newPosition) =>
        context.log.info(s"Updating position of boid $boidId to $newPosition")
        position = newPosition
        Behaviors.same

      case UpdateVelocity(newVelocity) =>
        context.log.info(s"Updating velocity of boid $boidId to $newVelocity")
        velocity = newVelocity
        Behaviors.same

