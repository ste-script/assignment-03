package pcd.ass03

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import pcd.ass01.Model.{P2d, V2d}

object SpacePartitionerActor:
  // Message protocol
  sealed trait Command

  final case class BoidTerminated(boidRef: ActorRef[BoidActor.Command]) extends Command

  final case class UpdateBoidPosition(boidRef: ActorRef[BoidActor.Command], position: P2d) extends Command

  final case class UpdateBoidVelocity(boidRef: ActorRef[BoidActor.Command], velocity: V2d) extends Command

  final case class FindNeighbors(
      boidRef: ActorRef[BoidActor.Command],
      position: P2d,
      perceptionRadius: Double
  ) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    var positions = Map.empty[ActorRef[BoidActor.Command], P2d]
    var velocities = Map.empty[ActorRef[BoidActor.Command], V2d]

    Behaviors.receiveMessage {
      case BoidTerminated(boidRef) =>
        positions = positions - boidRef
        velocities = velocities - boidRef
        Behaviors.same
      case UpdateBoidPosition(boidRef, position) =>
        positions = positions.updated(boidRef, position)
        Behaviors.same

      case UpdateBoidVelocity(boidRef, velocity) =>
        velocities = velocities.updated(boidRef, velocity)
        Behaviors.same

      case FindNeighbors(boidRef, position, perceptionRadius) =>
        if positions.size != velocities.size then
          ctx.log.warn(s"${positions.size} positions and ${velocities.size} velocities do not match!")
        val neighbors = for (r, p) <- positions if r != boidRef && p.distance(position) <= perceptionRadius
        yield (p, velocities.getOrElse(r, V2d(0, 0)))
        boidRef ! BoidActor.NeighborsResult(neighbors.toSeq)
        Behaviors.same
    }
  }
