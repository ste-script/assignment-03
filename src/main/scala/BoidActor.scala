import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.{Boid, BoidsModel, P2d, V2d}

import java.util
import java.util.List
import scala.util.Random


object BoidActor {
  sealed trait Command

  case object VelocityTick extends Command

  case object PositionTick extends Command

  private case class PositionUpdate(boidRef: ActorRef[Command], position: P2d) extends Command

  private case class VelocityUpdate(boidRef: ActorRef[Command], velocity: V2d) extends Command


  def apply(view: pcd.ass01.View.ScalaBoidsView): Behavior[Command] = Behaviors.setup { ctx =>
    var knownPositions = Map.empty[ActorRef[Command], P2d]
    var knownVelocities = Map.empty[ActorRef[Command], V2d]
    val perceptionRadius = 50.0 // Example perception radius
    val avoidRadius = 20.0 // Example avoidance radius
    val width = 1000
    val height =1000
    val maxSpeed = 4.0 // Example maximum speed
    val separationWeight = 1.0 // Weight for separation behavior
    val cohesionWeight = 1.0 // Weight for cohesion behavior
    val alignmentWeight = 1.0 // Weight for alignment behavior
    var position = P2d(-width / 2 + Random().nextDouble() * width, -height / 2 + Random().nextDouble() * height) // Initial position
    var velocity = V2d(Random().nextDouble() * maxSpeed / 2 - maxSpeed / 4, Random().nextDouble() * maxSpeed / 2 - maxSpeed / 4); // Initial velocity

    def minX = -width / 2

    def maxX = width / 2

    def minY = -height / 2

    def maxY = height / 2

    def findNeighbors(): Set[ActorRef[Command]] =
      knownPositions.keys.filter { boidRef =>
        position.distance(knownPositions(boidRef)) < perceptionRadius
      }.toSet

    def calculateSeparation(nearbyBoids: Set[ActorRef[Command]]): V2d =
      val (dx, dy, count) = nearbyBoids.foldLeft((0.0, 0.0, 0)) { case ((dxAcc, dyAcc, cnt), other) =>
        val otherPos = knownPositions(other)
        val distance = position.distance(otherPos)
        if (distance < avoidRadius) {
          (dxAcc + position.x - otherPos.x, dyAcc + position.y - otherPos.y, cnt + 1)
        } else {
          (dxAcc, dyAcc, cnt)
        }
      }
      if (count > 0) V2d(dx / count, dy / count).getNormalized else V2d(0, 0)

    def calculateCohesion(nearbyBoids: Set[ActorRef[Command]]): V2d =
      if (nearbyBoids.nonEmpty) {
        val (centerX, centerY) = nearbyBoids.foldLeft((0.0, 0.0)) { case ((xAcc, yAcc), other) =>
          val otherPos = knownPositions(other)
          (xAcc + otherPos.x, yAcc + otherPos.y)
        }
        val avgX = centerX / nearbyBoids.size
        val avgY = centerY / nearbyBoids.size
        V2d(avgX - position.x, avgY - position.y).getNormalized
      } else V2d(0, 0)


    def calculateAlignment(nearbyBoids: Set[ActorRef[Command]]): V2d =
      if (nearbyBoids.nonEmpty) {
        val (totalVx, totalVy) = nearbyBoids.foldLeft((0.0, 0.0)) { case ((vxAcc, vyAcc), other) =>
          val otherVel = knownVelocities(other)
          (vxAcc + otherVel.x, vyAcc + otherVel.y)
        }
        val avgVx = totalVx / nearbyBoids.size
        val avgVy = totalVy / nearbyBoids.size
        V2d(avgVx - velocity.x, avgVy - velocity.y).getNormalized
      } else V2d(0, 0)


    Behaviors.receiveMessage {
      case VelocityTick =>
        // Broadcast own position to all known boids
        knownVelocities.keys.foreach(_ ! VelocityUpdate(ctx.self, velocity))
        val neighbors = findNeighbors()
        val separation = calculateSeparation(neighbors)
        val cohesion = calculateCohesion(neighbors)
        val alignment = calculateAlignment(neighbors)
        velocity = velocity.sum(alignment.mul(alignmentWeight))
          .sum(separation.mul(separationWeight))
          .sum(cohesion.mul(cohesionWeight));
        val speed = velocity.abs()
        if (speed > maxSpeed) {
          velocity = velocity.getNormalized.mul(maxSpeed) // Limit speed
        }
        Behaviors.same
      case PositionTick =>
        // Broadcast own velocity to all known boids
        knownPositions.keys.foreach(_ ! PositionUpdate(ctx.self, position))
        position = position.sum(velocity) // Update position with velocity

        // Handle boundary wrapping
        position = position.sum(
          V2d(
            if (position.x < minX) width else if (position.x >= maxX) -width else 0,
            if (position.y < minY) height else if (position.y >= maxY) -height else 0
          )
        )

        view.updateMapPosition(ctx.self.path.name, V2d(position.x, position.y))
        Behaviors.same
      case PositionUpdate(ref, pos)
      =>
        knownPositions += (ref -> pos)
        Behaviors.same
      case VelocityUpdate(ref, vel)
      =>
        knownVelocities += (ref -> vel)
        Behaviors.same
    }
  }
}
