import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import pcd.ass01.Model.P2d
import scala.jdk.CollectionConverters._

object ViewActor {
  def apply(view: pcd.ass01.View.ScalaBoidsView): Behavior[BoidActor.Command] = Behaviors.setup { ctx =>
    var knownPositions = Seq.empty[P2d]
    Behaviors.receiveMessage {
      case BoidActor.PositionUpdate(ref, pos) =>
        knownPositions = pos +: knownPositions
        Behaviors.same
      case BoidActor.ViewTick =>
        view.updateAllMapPositions(knownPositions.asJava)
        knownPositions = Seq.empty[P2d] // Clear the positions after updating the view
        Behaviors.same
      case _ =>
        Behaviors.unhandled
    }
  }
}
