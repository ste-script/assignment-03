import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.{P2d, V2d}

object SpacePartitionerActor {
  // Message protocol
  sealed trait Command

  case object Clean extends Command

  final case class UpdateBoidPosition(boidRef: ActorRef[BoidActor.Command], position: P2d) extends Command

  final case class UpdateBoidVelocity(boidRef: ActorRef[BoidActor.Command], velocity: V2d) extends Command

  final case class FindNeighbors(
                                  boidRef: ActorRef[BoidActor.Command],
                                  position: P2d,
                                  perceptionRadius: Double
                                ) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    var positions = Seq.empty[(ActorRef[BoidActor.Command], P2d)]
    var velocities = Map.empty[ActorRef[BoidActor.Command], V2d]

    Behaviors.receiveMessage {
      case UpdateBoidPosition(boidRef, position) =>
        positions = (boidRef, position) +: positions
        Behaviors.same

      case UpdateBoidVelocity(boidRef, velocity) =>
        velocities = velocities.updated(boidRef, velocity)
        Behaviors.same

      case FindNeighbors(boidRef, position, perceptionRadius) =>
        if (positions.size != velocities.size) {
          ctx.log.warn(s"${positions.size} positions and ${velocities.size} velocities do not match!")
        }
        val neighbors = for n <- positions if n._1 != boidRef && n._2.distance(position) <= perceptionRadius
          yield (n._2, velocities.getOrElse(n._1, V2d(0, 0)))
        boidRef ! BoidActor.NeighborsResult(neighbors)
        Behaviors.same

      case Clean =>
        positions = positions.empty
        Behaviors.same

    }
  }
}