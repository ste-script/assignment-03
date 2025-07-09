import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import pcd.ass01.Model.P2d
import pcd.ass03.{BoidActor, BoidsSimulation, SpacePartitionerActor, ViewActor}
import pcd.ass03.View.BoidsView

class IntegrationTestSuite extends AnyFunSuite with BeforeAndAfterAll:
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("Complete boids simulation integration test") {
    // Create a mock view for testing
    var viewUpdateCount = 0
    var lastPositions: java.util.List[P2d] = null

    val mockView = new BoidsView(800, 600):
      override def updateAllMapPositions(positions: java.util.List[P2d]): Unit =
        lastPositions = positions

      override def update(fps: Int): Unit =
        viewUpdateCount += 1

    // Create the main actors
    val spacePartitioner = testKit.spawn(SpacePartitionerActor())
    val viewActor = testKit.spawn(ViewActor(mockView))
    val simulationProbe = testKit.createTestProbe[BoidsSimulation.Command]()

    // Create a few boids
    val boid1 = testKit.spawn(BoidActor(viewActor, spacePartitioner, simulationProbe.ref, 1.0, 1.0, 1.0))
    val boid2 = testKit.spawn(BoidActor(viewActor, spacePartitioner, simulationProbe.ref, 1.0, 1.0, 1.0))
    val boid3 = testKit.spawn(BoidActor(viewActor, spacePartitioner, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Start the view simulation
    viewActor ! ViewActor.SimulationStarted

    // Wait for initial setup messages
    Thread.sleep(100)

    // Simulate a complete update cycle
    // 1. Velocity tick - boids request neighbors
    boid1 ! BoidActor.VelocityTick
    boid2 ! BoidActor.VelocityTick
    boid3 ! BoidActor.VelocityTick

    // Wait for neighbor finding to complete
    Thread.sleep(50)

    // 2. Position tick - boids update positions
    boid1 ! BoidActor.PositionTick
    boid2 ! BoidActor.PositionTick
    boid3 ! BoidActor.PositionTick

    // 3. Trigger view update
    viewActor ! ViewActor.ViewTick(60)

    // Wait for all updates to process
    Thread.sleep(100)

    // Verify that the system processed the updates
    assert(viewUpdateCount > 0, "View should have been updated")

    // Test dynamic weight changes
    boid1 ! BoidActor.UpdateCohesionWeight(2.0)
    boid2 ! BoidActor.UpdateSeparationWeight(1.5)
    boid3 ! BoidActor.UpdateAlignmentWeight(0.8)

    // Run another update cycle with new weights
    boid1 ! BoidActor.VelocityTick
    boid2 ! BoidActor.VelocityTick
    boid3 ! BoidActor.VelocityTick

    Thread.sleep(50)

    boid1 ! BoidActor.PositionTick
    boid2 ! BoidActor.PositionTick
    boid3 ! BoidActor.PositionTick

    viewActor ! ViewActor.ViewTick(60)
    Thread.sleep(100)

    // Test termination
    boid1 ! BoidActor.Terminate
    Thread.sleep(50)

    // Final view update
    viewActor ! ViewActor.ViewTick(60)
    Thread.sleep(50)

    assert(viewUpdateCount >= 3, "Multiple view updates should have occurred")
  }

  test("Simulation state management integration") {
    val mockView = new BoidsView(800, 600)
    val viewActor = testKit.spawn(ViewActor(mockView))
    val spacePartitioner = testKit.spawn(SpacePartitionerActor())
    val simulationProbe = testKit.createTestProbe[BoidsSimulation.Command]()

    val boid = testKit.spawn(BoidActor(viewActor, spacePartitioner, simulationProbe.ref, 1.0, 1.0, 1.0))

    // Test simulation lifecycle
    // Start simulation
    viewActor ! ViewActor.SimulationStarted

    // Boid should be active and processing updates
    boid ! BoidActor.VelocityTick
    boid ! BoidActor.PositionTick
    viewActor ! ViewActor.ViewTick(60)

    Thread.sleep(100)

    // Pause simulation (stop view updates)
    viewActor ! ViewActor.SimulationStopped

    // Updates should now be ignored
    boid ! BoidActor.PositionTick
    viewActor ! ViewActor.ViewTick(60)

    // Restart simulation
    viewActor ! ViewActor.SimulationStarted

    // Updates should be processed again
    boid ! BoidActor.PositionTick
    viewActor ! ViewActor.ViewTick(60)

    Thread.sleep(100)

    // Clean termination
    boid ! BoidActor.Terminate
    Thread.sleep(50)
  }
