import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.{P2d, V2d}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import scala.util.Random

object BoidActor {
  // Configuration parameters
  private object Config {
    val Width = 1000
    val Height = 1000
    val PerceptionRadius = 50.0
    val AvoidRadius = 20.0
    val MaxSpeed = 4.0
    val SeparationWeight = 1.0
    val CohesionWeight = 1.0
    val AlignmentWeight = 1.0

    val MinX = -Width / 2
    val MaxX = Width / 2
    val MinY = -Height / 2
    val MaxY = Height / 2
  }

  // Message protocol
  sealed trait Command

  case object VelocityTick extends Command

  case object PositionTick extends Command

  case object ViewTick extends Command

  private final case class UpdatedBoidList(allBoids: Set[ActorRef[Command]]) extends Command

  final case class PositionUpdate(boidRef: ActorRef[Command], position: P2d) extends Command

  private case class VelocityUpdate(boidRef: ActorRef[Command], velocity: V2d) extends Command

  final case class NeighborsResult(neighbors: Seq[(P2d, V2d)]) extends Command

  private val BoidServiceKey: ServiceKey[Command] = ServiceKey[Command]("Boid")

  def apply(
             viewActor: ActorRef[Command],
             spacePartitioner: ActorRef[SpacePartitionerActor.Command]
           ): Behavior[Command] = Behaviors.setup { ctx =>
    import Config._
    val random = new Random()

    // State variables
    var allBoids = Set.empty[ActorRef[Command]]
    var position = P2d(
      MinX + random.nextDouble() * Width,
      MinY + random.nextDouble() * Height
    )

    var velocity = V2d(
      random.nextDouble() * MaxSpeed - MaxSpeed / 2,
      random.nextDouble() * MaxSpeed - MaxSpeed / 2
    )

    def calculateFlocking(neighbors: Seq[(P2d, V2d)]): V2d = {
      var separationX, separationY = 0.0
      var separationCount = 0
      var alignmentX, alignmentY = 0.0
      var cohesionX, cohesionY = 0.0
      neighbors.foreach { case (pos, vel) =>
        val distance = position.distance(pos)

        // Separation - only consider close neighbors
        if (distance < AvoidRadius && distance > 0) {
          separationX += (position.x - pos.x) / (distance * distance)
          separationY += (position.y - pos.y) / (distance * distance)
          separationCount += 1
        }

        // Alignment - consider velocity
        alignmentX += vel.x
        alignmentY += vel.y

        // Cohesion - consider position
        cohesionX += pos.x
        cohesionY += pos.y
      }

      // Calculate final vectors
      val separation = if (separationCount > 0) {
        V2d(separationX, separationY).getNormalized
      } else V2d(0, 0)

      val alignment = V2d(
        alignmentX / neighbors.size - velocity.x,
        alignmentY / neighbors.size - velocity.y
      ).getNormalized

      val center = P2d(cohesionX / neighbors.size, cohesionY / neighbors.size)
      val cohesion = V2d(center.x - position.x, center.y - position.y).getNormalized

      // Combine forces
      val newVelocity = velocity
        .sum(separation.mul(SeparationWeight))
        .sum(alignment.mul(AlignmentWeight))
        .sum(cohesion.mul(CohesionWeight))

      // Limit speed
      val speed = newVelocity.abs()
      if (speed > MaxSpeed) newVelocity.getNormalized.mul(MaxSpeed) else newVelocity
    }

    // Helper for boundary wrapping
    def wrapPosition(pos: P2d): P2d = {
      val x = if (pos.x < MinX) pos.x + Width else if (pos.x > MaxX) pos.x - Width else pos.x
      val y = if (pos.y < MinY) pos.y + Height else if (pos.y > MaxY) pos.y - Height else pos.y
      P2d(x, y)
    }

    // Register with receptionist
    ctx.system.receptionist ! Receptionist.Register(BoidServiceKey, ctx.self)

    val adapter = ctx.messageAdapter[Receptionist.Listing] {
      case listing if listing.isForKey(BoidServiceKey) =>
        UpdatedBoidList(listing.serviceInstances(BoidServiceKey))
    }
    ctx.system.receptionist ! Receptionist.Subscribe(BoidServiceKey, adapter)

    // Initialize the space partitioner with our position and velocity
    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(ctx.self, velocity)
    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(ctx.self, position)

    Behaviors.receiveMessage {
      case VelocityTick =>
        spacePartitioner ! SpacePartitionerActor.FindNeighbors(
          ctx.self, position, PerceptionRadius
        )
        Behaviors.same

      case NeighborsResult(neighbors) =>
        val newVelocity = calculateFlocking(neighbors)
        velocity = newVelocity
        spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(ctx.self, velocity)
        allBoids.foreach(_ ! VelocityUpdate(ctx.self, velocity))
        Behaviors.same

      case PositionTick =>
        position = wrapPosition(position.sum(velocity))
        spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(ctx.self, position)
        allBoids.foreach(_ ! PositionUpdate(ctx.self, position))
        viewActor ! PositionUpdate(ctx.self, position)
        Behaviors.same

      case UpdatedBoidList(boids) =>
        allBoids = boids - ctx.self
        Behaviors.same

      case _ => Behaviors.unhandled
    }
  }
}