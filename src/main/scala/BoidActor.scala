import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

object BoidActor:
  sealed trait Command
  // Define the messages that the Boid actor can handle
  final case class UpdatePosition(replyTo: ActorRef[BoidState]) extends Command
  final case class UpdateVelocity(replyTo: ActorRef[BoidState]) extends Command
  final case class GetState(replyTo: ActorRef[BoidState]) extends Command
  final case class BoidState(position: (Double, Double), velocity: (Double, Double))

  // Define the state of a boid

  // The behavior of the Boid actor
  def apply(): Behavior[Command] = Behaviors.setup: context =>
    val boidId = context.self.path.name
    val state = BoidState((0.0, 0.0), (0.0, 0.0))


    Behaviors.receiveMessage:
      case UpdatePosition(replyTo) =>
        context.log.info(s"Updating position of boid $boidId")
        replyTo ! state
        Behaviors.same

      case UpdateVelocity(replyTo) =>
        context.log.info(s"Updating velocity of boid $boidId")
        replyTo ! state
        Behaviors.same

      case GetState(replyTo) =>
        context.log.info(s"Getting state of boid $boidId")
        replyTo ! state
        Behaviors.same
