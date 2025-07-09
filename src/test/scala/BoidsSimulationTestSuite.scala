import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import pcd.ass03.{BoidsSimulation, BoidActor}

class BoidsSimulationTestSuite extends AnyFunSuite with BeforeAndAfterAll:
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("BoidsSimulation should start in terminated state and transition to running") {
    val simulation = testKit.spawn(BoidsSimulation())

    // Should start in terminated state - other messages should be ignored
    simulation ! BoidsSimulation.PauseSimulation
    simulation ! BoidsSimulation.ResumeSimulation

    // Start simulation should work
    simulation ! BoidsSimulation.StartSimulation

    // Now it should be running - we can't directly verify state but can test transitions
    simulation ! BoidsSimulation.PauseSimulation
    simulation ! BoidsSimulation.ResumeSimulation
  }

  test("BoidsSimulation should handle weight updates") {
    val simulation = testKit.spawn(BoidsSimulation())

    // Should accept weight updates in any state
    simulation ! BoidsSimulation.UpdateCohesionWeight(2.0)
    simulation ! BoidsSimulation.UpdateSeparationWeight(1.5)
    simulation ! BoidsSimulation.UpdateAlignmentWeight(3.0)

    // Start simulation to create boids that will receive these weights
    simulation ! BoidsSimulation.StartSimulation

    // Update weights while running
    simulation ! BoidsSimulation.UpdateCohesionWeight(2.5)
    simulation ! BoidsSimulation.UpdateSeparationWeight(2.0)
    simulation ! BoidsSimulation.UpdateAlignmentWeight(1.0)
  }

  test("BoidsSimulation should handle number of boids updates") {
    val simulation = testKit.spawn(BoidsSimulation())

    // Should accept boid count updates
    simulation ! BoidsSimulation.UpdateNumberOfBoids(100)
    simulation ! BoidsSimulation.UpdateNumberOfBoids(50)

    // Start simulation
    simulation ! BoidsSimulation.StartSimulation

    // Update while running (should dynamically adjust boid count)
    simulation ! BoidsSimulation.UpdateNumberOfBoids(75)
    simulation ! BoidsSimulation.UpdateNumberOfBoids(25)
  }

  test("BoidsSimulation should coordinate boid velocity and position updates") {
    val simulation = testKit.spawn(BoidsSimulation())
    val mockBoidRef = testKit.createTestProbe[BoidActor.Command]().ref

    simulation ! BoidsSimulation.StartSimulation

    // Simulate boid update notifications
    simulation ! BoidsSimulation.BoidVelocityUpdated(mockBoidRef)
    simulation ! BoidsSimulation.BoidPositionUpdated(mockBoidRef)
  }

  test("BoidsSimulation should handle pause and resume properly") {
    val simulation = testKit.spawn(BoidsSimulation())

    // Start simulation
    simulation ! BoidsSimulation.StartSimulation

    // Pause
    simulation ! BoidsSimulation.PauseSimulation

    // Try to update while paused
    simulation ! BoidsSimulation.UpdateCohesionWeight(3.0)

    // Resume
    simulation ! BoidsSimulation.ResumeSimulation

    // Should be running again
    simulation ! BoidsSimulation.UpdateAlignmentWeight(2.0)
  }

  test("BoidsSimulation should handle termination") {
    val simulation = testKit.spawn(BoidsSimulation())

    // Start simulation
    simulation ! BoidsSimulation.StartSimulation

    // Terminate
    simulation ! BoidsSimulation.TerminateSimulation

    // Actor should stop - we can't directly test this without more complex setup
    // but the message should be handled without errors
  }

  test("BoidsSimulation state transitions should be consistent") {
    val simulation = testKit.spawn(BoidsSimulation())

    // Test various state transitions
    simulation ! BoidsSimulation.UpdateNumberOfBoids(10)
    simulation ! BoidsSimulation.StartSimulation
    simulation ! BoidsSimulation.UpdateCohesionWeight(1.5)
    simulation ! BoidsSimulation.PauseSimulation
    simulation ! BoidsSimulation.UpdateSeparationWeight(2.0)
    simulation ! BoidsSimulation.ResumeSimulation
    simulation ! BoidsSimulation.UpdateAlignmentWeight(0.8)
    simulation ! BoidsSimulation.TerminateSimulation
  }

  test("BoidsSimulation should handle multiple boid updates in sequence") {
    val simulation = testKit.spawn(BoidsSimulation())
    val boidRefs = (1 to 5).map(_ => testKit.createTestProbe[BoidActor.Command]().ref)

    simulation ! BoidsSimulation.StartSimulation

    // Simulate multiple boids updating velocities
    boidRefs.foreach(ref => simulation ! BoidsSimulation.BoidVelocityUpdated(ref))

    // Simulate multiple boids updating positions
    boidRefs.foreach(ref => simulation ! BoidsSimulation.BoidPositionUpdated(ref))
  }
