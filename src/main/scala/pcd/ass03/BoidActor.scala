package pcd.ass03

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import pcd.ass01.Model.{P2d, V2d}

import scala.util.Random

object BoidActor:
  // Configuration parameters
  private object Config:
    val Width = 1000
    val Height = 1000
    val PerceptionRadius = 50.0
    val AvoidRadius = 20.0
    val MaxSpeed = 4.0
    val MinX: Int = -Width / 2
    val MaxX: Int = Width / 2
    val MinY: Int = -Height / 2
    val MaxY: Int = Height / 2

  // Message protocol
  sealed trait Command

  case object VelocityTick extends Command

  case object PositionTick extends Command

  case object Terminate extends Command

  final case class NeighborsResult(neighbors: Seq[(P2d, V2d)]) extends Command
  final case class UpdateCohesionWeight(cohesionWeight: Double) extends Command
  final case class UpdateSeparationWeight(separationWeight: Double) extends Command
  final case class UpdateAlignmentWeight(alignmentWeight: Double) extends Command

  def apply(
      viewActor: ActorRef[ViewActor.Command],
      spacePartitioner: ActorRef[SpacePartitionerActor.Command],
      boidSimulation: ActorRef[BoidsSimulation.Command],
      cohesionWeight: Double,
      separationWeight: Double,
      alignmentWeight: Double
  ): Behavior[Command] = Behaviors.setup { ctx =>
    import Config.*
    val random = new Random()
    var _cohesionWeight = cohesionWeight
    var _separationWeight = separationWeight
    var _alignmentWeight = alignmentWeight

    // State variables
    var _position = P2d(
      MinX + random.nextDouble() * Width,
      MinY + random.nextDouble() * Height
    )

    var _velocity = V2d(
      random.nextDouble() * MaxSpeed - MaxSpeed / 2,
      random.nextDouble() * MaxSpeed - MaxSpeed / 2
    )

    def calculateSeparation(nearbyBoids: Seq[(P2d, V2d)]) =
      var dx: Double = 0
      var dy: Double = 0
      var count = 0
      for other <- nearbyBoids do
        val otherPos = other._1
        val distance = _position.distance(otherPos)
        if distance < AvoidRadius then
          dx += _position.x - otherPos.x
          dy += _position.y - otherPos.y
          count += 1
      if count > 0 then
        dx /= count
        dy /= count
        V2d(dx, dy).getNormalized
      else V2d(0, 0)

    def calculateCohesion(nearbyBoids: Seq[(P2d, V2d)]) =
      var centerX: Double = 0
      var centerY: Double = 0
      if nearbyBoids.nonEmpty then
        for other <- nearbyBoids do
          val otherPos = other._1
          centerX += otherPos.x
          centerY += otherPos.y
        centerX /= nearbyBoids.size
        centerY /= nearbyBoids.size
        V2d(centerX - _position.x, centerY - _position.y).getNormalized
      else V2d(0, 0)

    def calculateAlignment(nearbyBoids: Seq[(P2d, V2d)]) =
      var avgVx: Double = 0
      var avgVy: Double = 0
      if nearbyBoids.nonEmpty then
        for other <- nearbyBoids do
          val otherVel = other._2
          avgVx += otherVel.x
          avgVy += otherVel.y
        avgVx /= nearbyBoids.size
        avgVy /= nearbyBoids.size
        V2d(avgVx - _velocity.x, avgVy - _velocity.y).getNormalized
      else V2d(0, 0)

    def newVelocity(neighbors: Seq[(P2d, V2d)]): V2d =
      val separation: V2d = calculateSeparation(neighbors)
      val alignment: V2d = calculateAlignment(neighbors)
      val cohesion: V2d = calculateCohesion(neighbors)
      val v = _velocity
        .sum(
          alignment.mul(_alignmentWeight)
        )
        .sum(separation.mul(_separationWeight))
        .sum(
          cohesion.mul(_cohesionWeight)
        )
      if v.abs() > MaxSpeed then v.getNormalized.mul(MaxSpeed) else v

    // Helper for boundary wrapping
    def wrapPosition(pos: P2d): P2d =
      var p = pos
      if pos.x < MinX then p = p.sum(new V2d(Width, 0))
      if pos.x >= MaxX then p = p.sum(new V2d(-Width, 0))
      if pos.y < MinY then p = p.sum(new V2d(0, Height))
      if pos.y >= MaxY then p = p.sum(new V2d(0, -Height))
      p

    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(ctx.self, _velocity)
    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(ctx.self, _position)

    Behaviors.receiveMessage {
      case VelocityTick =>
        spacePartitioner ! SpacePartitionerActor.FindNeighbors(
          ctx.self,
          _position,
          PerceptionRadius
        )
        Behaviors.same

      case NeighborsResult(neighbors) =>
        _velocity = newVelocity(neighbors)
        spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(ctx.self, _velocity)
        boidSimulation ! BoidsSimulation.BoidVelocityUpdated(ctx.self)
        Behaviors.same

      case PositionTick =>
        _position = wrapPosition(_position.sum(_velocity))
        spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(ctx.self, _position)
        viewActor ! ViewActor.BoidPositionUpdate(ctx.self, _position)
        boidSimulation ! BoidsSimulation.BoidPositionUpdated(ctx.self)
        Behaviors.same

      case Terminate =>
        spacePartitioner ! SpacePartitionerActor.BoidTerminated(ctx.self)
        viewActor ! ViewActor.BoidTerminated(ctx.self)
        Behaviors.stopped

      case UpdateCohesionWeight(cohesionWeight) =>
        _cohesionWeight = cohesionWeight
        Behaviors.same
      case UpdateSeparationWeight(separationWeight) =>
        _separationWeight = separationWeight
        Behaviors.same
      case UpdateAlignmentWeight(alignmentWeight) =>
        _alignmentWeight = alignmentWeight
        Behaviors.same

    }
  }
