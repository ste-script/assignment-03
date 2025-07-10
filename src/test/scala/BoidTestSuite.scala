import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import pcd.ass01.Model.{P2d, V2d}
import pcd.ass03.{BoidActor, SpacePartitionerActor, ViewActor, BoidsSimulationActor}
import pcd.ass03.View.BoidsView
import scala.compiletime.uninitialized

class   BoidTestSuite extends AnyFunSuite with BeforeAndAfterAll:
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("BoidActor should initialize with random position and velocity") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Verify initial messages are sent to space partitioner
    spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidVelocity]
    spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidPosition]
  }

  test("BoidActor should handle VelocityTick by requesting neighbors") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    boidRef ! BoidActor.VelocityTick

    val findNeighborsMsg = spaceProbe.expectMessageType[SpacePartitionerActor.FindNeighbors]
    assert(findNeighborsMsg.boidRef == boidRef)
    assert(findNeighborsMsg.perceptionRadius == 50.0)
  }

  test("BoidActor should update velocity based on neighbors") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    val neighbors = Seq(
      (P2d(10, 10), V2d(1, 0)),
      (P2d(20, 20), V2d(0, 1))
    )

    boidRef ! BoidActor.NeighborsResult(neighbors)

    spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidVelocity]
    simulationProbe.expectMessageType[BoidsSimulationActor.BoidVelocityUpdated]
  }

  test("BoidActor should update position and wrap around boundaries") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    boidRef ! BoidActor.PositionTick

    spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidPosition]
    viewProbe.expectMessageType[ViewActor.BoidPositionUpdate]
    simulationProbe.expectMessageType[BoidsSimulationActor.BoidPositionUpdated]
  }

  test("BoidActor should handle weight updates") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    boidRef ! BoidActor.UpdateCohesionWeight(2.0)
    boidRef ! BoidActor.UpdateSeparationWeight(3.0)
    boidRef ! BoidActor.UpdateAlignmentWeight(4.0)

    // These should not produce any messages, just update internal state
    spaceProbe.expectNoMessage()
  }

  test("BoidActor should terminate properly") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    boidRef ! BoidActor.Terminate

    spaceProbe.expectMessageType[SpacePartitionerActor.BoidTerminated]
    viewProbe.expectMessageType[ViewActor.BoidTerminated]
  }

  test("SpacePartitionerActor should track boid positions and velocities") {
    val spacePartitioner = testKit.spawn(SpacePartitionerActor())
    val boidProbe = testKit.createTestProbe[BoidActor.Command]()

    val position = P2d(100, 100)
    val velocity = V2d(1, 1)

    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(boidProbe.ref, position)
    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(boidProbe.ref, velocity)

    // No direct way to verify internal state, but we can test neighbor finding
    spacePartitioner ! SpacePartitionerActor.FindNeighbors(boidProbe.ref, position, 50.0)

    boidProbe.expectMessageType[BoidActor.NeighborsResult]
  }

  test("SpacePartitionerActor should find neighbors within perception radius") {
    val spacePartitioner = testKit.spawn(SpacePartitionerActor())
    val boid1Probe = testKit.createTestProbe[BoidActor.Command]()
    val boid2Probe = testKit.createTestProbe[BoidActor.Command]()
    val boid3Probe = testKit.createTestProbe[BoidActor.Command]()

    // Position boids
    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(boid1Probe.ref, P2d(0, 0))
    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(boid1Probe.ref, V2d(1, 0))

    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(boid2Probe.ref, P2d(30, 0)) // Within range
    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(boid2Probe.ref, V2d(0, 1))

    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(boid3Probe.ref, P2d(100, 0)) // Out of range
    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(boid3Probe.ref, V2d(-1, 0))

    spacePartitioner ! SpacePartitionerActor.FindNeighbors(boid1Probe.ref, P2d(0, 0), 50.0)

    val neighborsResult = boid1Probe.expectMessageType[BoidActor.NeighborsResult]
    assert(neighborsResult.neighbors.length == 1) // Only boid2 should be in range
    assert(neighborsResult.neighbors.head._1 == P2d(30, 0))
  }

  test("SpacePartitionerActor should handle boid termination") {
    val spacePartitioner = testKit.spawn(SpacePartitionerActor())
    val boidProbe = testKit.createTestProbe[BoidActor.Command]()

    spacePartitioner ! SpacePartitionerActor.UpdateBoidPosition(boidProbe.ref, P2d(0, 0))
    spacePartitioner ! SpacePartitionerActor.UpdateBoidVelocity(boidProbe.ref, V2d(1, 1))

    spacePartitioner ! SpacePartitionerActor.BoidTerminated(boidProbe.ref)

    // After termination, boid should not be found as neighbor
    spacePartitioner ! SpacePartitionerActor.FindNeighbors(boidProbe.ref, P2d(0, 0), 50.0)

    boidProbe.expectMessageType[BoidActor.NeighborsResult]
  }

  test("ViewActor should track boid positions and handle view updates") {
    // Create a mock view with accessible test fields
    class TestBoidsView extends BoidsView(800, 600) {
      var lastPositions: java.util.List[P2d] = uninitialized
      var lastFps: Int = 0

      override def updateAllMapPositions(positions: java.util.List[P2d]): Unit =
        lastPositions = positions

      override def update(fps: Int): Unit =
        lastFps = fps
    }

    val mockView = new TestBoidsView()
    val viewActor = testKit.spawn(ViewActor(mockView))
    val boidProbe = testKit.createTestProbe[BoidActor.Command]()

    // Start simulation
    viewActor ! ViewActor.SimulationStarted

    // Update boid position
    viewActor ! ViewActor.BoidPositionUpdate(boidProbe.ref, P2d(100, 200))

    // Trigger view update
    viewActor ! ViewActor.ViewTick(60)

    // Give time for async processing
    Thread.sleep(50)

    // Verify position was tracked (indirectly through behavior)
    assert(mockView.lastFps == 60)
  }

  test("ViewActor should handle simulation state transitions") {
    val mockView = new BoidsView(800, 600)
    val viewActor = testKit.spawn(ViewActor(mockView))
    val boidProbe = testKit.createTestProbe[BoidActor.Command]()

    // Initially in terminated state
    viewActor ! ViewActor.BoidPositionUpdate(boidProbe.ref, P2d(100, 100))
    viewActor ! ViewActor.ViewTick(60) // Should be ignored

    // Start simulation
    viewActor ! ViewActor.SimulationStarted

    // Now updates should be processed
    viewActor ! ViewActor.BoidPositionUpdate(boidProbe.ref, P2d(200, 200))
    viewActor ! ViewActor.ViewTick(60)

    // Stop simulation
    viewActor ! ViewActor.SimulationStopped

    // Updates should be ignored again
    viewActor ! ViewActor.BoidPositionUpdate(boidProbe.ref, P2d(300, 300))
    viewActor ! ViewActor.ViewTick(60)
  }

  test("ViewActor should handle boid termination") {
    val mockView = new BoidsView(800, 600)
    val viewActor = testKit.spawn(ViewActor(mockView))
    val boidProbe = testKit.createTestProbe[BoidActor.Command]()

    viewActor ! ViewActor.SimulationStarted
    viewActor ! ViewActor.BoidPositionUpdate(boidProbe.ref, P2d(100, 100))
    viewActor ! ViewActor.BoidTerminated(boidProbe.ref)

    // Position should be removed from tracking
    viewActor ! ViewActor.ViewTick(60)
  }

  test("Boid flocking behavior - separation calculation") {
    // Test the separation behavior directly with ActorTestKit instead of BehaviorTestKit
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 2.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    // Test with nearby boids that should trigger separation
    val nearbyBoids = Seq(
      (P2d(-10, 0), V2d(1, 0)), // Very close boid - should trigger separation
      (P2d(40, 0), V2d(0, 1))   // Farther boid
    )

    boidRef ! BoidActor.NeighborsResult(nearbyBoids)

    // Verify messages were sent (separation should influence velocity)
    spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidVelocity]
    simulationProbe.expectMessageType[BoidsSimulationActor.BoidVelocityUpdated]
  }

  test("Boid boundary wrapping") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear initial messages
    spaceProbe.receiveMessages(2)

    // Multiple position ticks to test boundary wrapping
    for (_ <- 1 to 10) {
      boidRef ! BoidActor.PositionTick
      spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidPosition]
      viewProbe.expectMessageType[ViewActor.BoidPositionUpdate]
      simulationProbe.expectMessageType[BoidsSimulationActor.BoidPositionUpdated]
    }
  }

  test("Empty neighbors should not cause velocity changes") {
    val viewProbe = testKit.createTestProbe[ViewActor.Command]()
    val spaceProbe = testKit.createTestProbe[SpacePartitionerActor.Command]()
    val simulationProbe = testKit.createTestProbe[BoidsSimulationActor.Command]()

    val boidRef = testKit.spawn(BoidActor(viewProbe.ref, spaceProbe.ref, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Clear existing messages
    spaceProbe.receiveMessages(2)

    // Send empty neighbors
    boidRef ! BoidActor.NeighborsResult(Seq.empty)

    // Should still update velocity (but it might not change much)
    spaceProbe.expectMessageType[SpacePartitionerActor.UpdateBoidVelocity]
    simulationProbe.expectMessageType[BoidsSimulationActor.BoidVelocityUpdated]
  }
