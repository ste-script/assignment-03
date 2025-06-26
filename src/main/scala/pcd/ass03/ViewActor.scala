package pcd.ass03

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.P2d
import pcd.ass01.View.ScalaBoidsView

import scala.jdk.CollectionConverters.*

object ViewActor {
  sealed trait Command

  final case class BoidPositionUpdate(ref: ActorRef[BoidActor.Command], pos: P2d) extends Command

  case object ViewTick extends Command

  final case class BoidTerminated(ref: ActorRef[BoidActor.Command]) extends Command

  def apply(view: ScalaBoidsView): Behavior[ViewActor.Command] = Behaviors.setup { ctx =>
    var knownPositions = Map.empty[ActorRef[BoidActor.Command], P2d]
    Behaviors.receiveMessage {
      case ViewActor.BoidPositionUpdate(ref, pos) =>
        knownPositions = knownPositions.updated(ref, pos)
        Behaviors.same
      case ViewActor.ViewTick =>
        view.updateAllMapPositions(knownPositions.values.toList.asJava)
        Behaviors.same
      case ViewActor.BoidTerminated(ref) =>
        knownPositions = knownPositions - ref
        Behaviors.same
    }
  }
}
