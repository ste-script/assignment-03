import akka.actor.typed.ActorRef

final case class BoidState(position: (Double, Double), velocity: (Double, Double))

object BoidMessages:
  // Messages for the Boid actor
  sealed trait BoidMessage()

  // Message to update the position of a boid
  final case class UpdatePosition(newPosition: (Double, Double)) extends BoidMessage

  // Message to update the velocity of a boid
  final case class UpdateVelocity(newVelocity: (Double, Double)) extends BoidMessage

  final case class GetState(replyTo: ActorRef[BoidState]) extends BoidMessage
