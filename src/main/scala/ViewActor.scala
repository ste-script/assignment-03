import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import pcd.ass01.Model.P2d
import scala.jdk.CollectionConverters._

object ViewActor {
  def apply(view: pcd.ass01.View.ScalaBoidsView): Behavior[BoidActor.Command] = Behaviors.setup { ctx =>
    var knownPositions = Map.empty[String, P2d]
    Behaviors.receiveMessage {
      case BoidActor.PositionUpdate(ref, pos) =>
        knownPositions += (ref.path.name -> pos)
        Behaviors.same
      case BoidActor.ViewTick =>
        view.updateAllMapPositions(knownPositions.asJava)
        Behaviors.same
      case _ =>
        Behaviors.unhandled
    }
  }
}
