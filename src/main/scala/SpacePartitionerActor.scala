import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.{P2d, V2d}

object SpacePartitionerActor {
  // Message protocol
  sealed trait Command
  final case class UpdateBoidPosition(boidRef: ActorRef[BoidActor.Command], position: P2d) extends Command
  final case class UpdateBoidVelocity(boidRef: ActorRef[BoidActor.Command], velocity: V2d) extends Command
  final case class FindNeighbors(
                                  boidRef: ActorRef[BoidActor.Command],
                                  position: P2d,
                                  perceptionRadius: Double
                                ) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    var knownBoids = Map.empty[ActorRef[BoidActor.Command], (P2d, V2d)]

    Behaviors.receiveMessage {
      case UpdateBoidPosition(boidRef, position) =>
        knownBoids = knownBoids.updated(boidRef,
          (position, knownBoids.getOrElse(boidRef, (P2d(0, 0), V2d(0, 0)))._2))
        Behaviors.same

      case UpdateBoidVelocity(boidRef, velocity) =>
        knownBoids = knownBoids.updated(boidRef,
          (knownBoids.getOrElse(boidRef, (P2d(0, 0), V2d(0, 0)))._1, velocity))
        Behaviors.same

      case FindNeighbors(boidRef, position, perceptionRadius) =>
        val neighbors = knownBoids.filter { case (ref, (pos, _)) =>
          ref != boidRef && position.distance(pos) < perceptionRadius
        }.map { case (_, (pos, vel)) =>
          (pos, vel)
        }.toSeq

        boidRef ! BoidActor.NeighborsResult(neighbors)
        Behaviors.same
    }
  }
}