import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.V2d

case class Position(x: Double, y: Double)

case class Velocity(value: Double)

object BoidActor {
  sealed trait Command

  case object VelocityTick extends Command

  case object PositionTick extends Command

  private case class PositionUpdate(boidRef: ActorRef[Command], position: Position) extends Command

  private case class VelocityUpdate(boidRef: ActorRef[Command], velocity: Velocity) extends Command


  def apply(view: pcd.ass01.View.ScalaBoidsView): Behavior[Command] = Behaviors.setup { ctx =>
    var position = Position(0.0, 0.0) // Initial position
    var velocity = Velocity(0.0) // Initial velocity
    var knownPositions = Map.empty[ActorRef[Command], Position]
    var knownVelocities = Map.empty[ActorRef[Command], Velocity]

    def findNeighbors(): Seq[ActorRef[Command]] = {
      //todo
      knownPositions.keys.toSeq
    }

    Behaviors.receiveMessage {
      case VelocityTick =>
        // Broadcast own position to all known boids
        knownVelocities.keys.foreach(_ ! VelocityUpdate(ctx.self, velocity))
        val neighbors = findNeighbors()
        velocity = Velocity(1.0) // Example velocity update logic
        // Compute steering (placeholder)
        // Update velocity and position
        Behaviors.same
      case PositionTick =>
        // Broadcast own velocity to all known boids
        knownPositions.keys.foreach(_ ! PositionUpdate(ctx.self, position))
        val neighbors = findNeighbors()
        position = Position(position.x + velocity.value, position.y + velocity.value) // Example position update logic
        // Compute steering (placeholder)
        // Update position based on velocity
        view.updateMapPosition(ctx.self.path.name, V2d(position.x, position.y))
        Behaviors.same
      case PositionUpdate(ref, pos) =>
        knownPositions += (ref -> pos)
        Behaviors.same
      case VelocityUpdate(ref, vel) =>
        knownVelocities += (ref -> vel)
        Behaviors.same
    }
  }
}
