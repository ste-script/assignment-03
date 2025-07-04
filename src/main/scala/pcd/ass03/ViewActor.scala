package pcd.ass03

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import pcd.ass01.Model.P2d
import pcd.ass01.View.ScalaBoidsView

import scala.jdk.CollectionConverters.*

object ViewActor:
  sealed trait Command

  final case class BoidPositionUpdate(ref: ActorRef[BoidActor.Command], pos: P2d) extends Command

  final case class ViewTick(fps: Int) extends Command

  final case class BoidTerminated(ref: ActorRef[BoidActor.Command]) extends Command

  case object SimulationStarted extends Command

  case object SimulationStopped extends Command

  def apply(view: ScalaBoidsView): Behavior[Command] = Behaviors.setup: _ =>
    var boidsPositions: Map[ActorRef[BoidActor.Command], P2d] = Map.empty
    var isSimulationRunning = false // Track simulation state

    def terminatedState: Behavior[Command] = Behaviors.receiveMessage:
      case SimulationStarted =>
        isSimulationRunning = true
        boidsPositions = Map.empty // Clear any existing positions
        view.updateAllMapPositions(List.empty.asJava) // Clear the view
        runningState
      case _ =>
        // In the terminated state, we ignore all messages except SimulationStarted
        Behaviors.same

    def runningState: Behavior[Command] =
      Behaviors.receiveMessage:
        case BoidPositionUpdate(boidRef, position) =>
          // Only update positions if simulation is running
          if isSimulationRunning then boidsPositions = boidsPositions.updated(boidRef, position)
          Behaviors.same

        case BoidTerminated(boidRef) =>
          boidsPositions = boidsPositions.removed(boidRef)
          Behaviors.same

        case ViewTick(fps) =>
          if isSimulationRunning then view.updateAllMapPositions(boidsPositions.values.toList.asJava)
          view.update(fps)
          Behaviors.same

        case SimulationStopped =>
          isSimulationRunning = false
          boidsPositions = Map.empty // Clear all positions
          view.updateAllMapPositions(List.empty.asJava) // Clear the view immediately
          terminatedState
        case _ =>
          Behaviors.same

    terminatedState
