import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll
import pcd.ass03.{BoidsSimulationActor, BoidActor}

class BoidsSimulationActorTestSuite extends AnyFunSuite with BeforeAndAfterAll:
  val testKit: ActorTestKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  test("BoidsSimulation should start in terminated state and transition to running") {
    val simulation = testKit.spawn(BoidsSimulationActor())

    // Should start in terminated state - other messages should be ignored
    simulation ! BoidsSimulationActor.PauseSimulation
    simulation ! BoidsSimulationActor.ResumeSimulation

    // Start simulation should work
    simulation ! BoidsSimulationActor.StartSimulation

    // Now it should be running - we can't directly verify state but can test transitions
    simulation ! BoidsSimulationActor.PauseSimulation
    simulation ! BoidsSimulationActor.ResumeSimulation
  }

  test("BoidsSimulation should handle weight updates") {
    val simulation = testKit.spawn(BoidsSimulationActor())

    // Should accept weight updates in any state
    simulation ! BoidsSimulationActor.UpdateCohesionWeight(2.0)
    simulation ! BoidsSimulationActor.UpdateSeparationWeight(1.5)
    simulation ! BoidsSimulationActor.UpdateAlignmentWeight(3.0)

    // Start simulation to create boids that will receive these weights
    simulation ! BoidsSimulationActor.StartSimulation

    // Update weights while running
    simulation ! BoidsSimulationActor.UpdateCohesionWeight(2.5)
    simulation ! BoidsSimulationActor.UpdateSeparationWeight(2.0)
    simulation ! BoidsSimulationActor.UpdateAlignmentWeight(1.0)
  }

  test("BoidsSimulation should handle number of boids updates") {
    val simulation = testKit.spawn(BoidsSimulationActor())

    // Should accept boid count updates
    simulation ! BoidsSimulationActor.UpdateNumberOfBoids(100)
    simulation ! BoidsSimulationActor.UpdateNumberOfBoids(50)

    // Start simulation
    simulation ! BoidsSimulationActor.StartSimulation

    // Update while running (should dynamically adjust boid count)
    simulation ! BoidsSimulationActor.UpdateNumberOfBoids(75)
    simulation ! BoidsSimulationActor.UpdateNumberOfBoids(25)
  }

  test("BoidsSimulation should coordinate boid velocity and position updates") {
    val simulation = testKit.spawn(BoidsSimulationActor())
    val mockBoidRef = testKit.createTestProbe[BoidActor.Command]().ref

    simulation ! BoidsSimulationActor.StartSimulation

    // Simulate boid update notifications
    simulation ! BoidsSimulationActor.BoidVelocityUpdated(mockBoidRef)
    simulation ! BoidsSimulationActor.BoidPositionUpdated(mockBoidRef)
  }

  test("BoidsSimulation should handle pause and resume properly") {
    val simulation = testKit.spawn(BoidsSimulationActor())

    // Start simulation
    simulation ! BoidsSimulationActor.StartSimulation

    // Pause
    simulation ! BoidsSimulationActor.PauseSimulation

    // Try to update while paused
    simulation ! BoidsSimulationActor.UpdateCohesionWeight(3.0)

    // Resume
    simulation ! BoidsSimulationActor.ResumeSimulation

    // Should be running again
    simulation ! BoidsSimulationActor.UpdateAlignmentWeight(2.0)
  }

  test("BoidsSimulation should handle termination") {
    val simulation = testKit.spawn(BoidsSimulationActor())

    // Start simulation
    simulation ! BoidsSimulationActor.StartSimulation

    // Terminate
    simulation ! BoidsSimulationActor.TerminateSimulation

    // Actor should stop - we can't directly test this without more complex setup
    // but the message should be handled without errors
  }

  test("BoidsSimulation state transitions should be consistent") {
    val simulation = testKit.spawn(BoidsSimulationActor())

    // Test various state transitions
    simulation ! BoidsSimulationActor.UpdateNumberOfBoids(10)
    simulation ! BoidsSimulationActor.StartSimulation
    simulation ! BoidsSimulationActor.UpdateCohesionWeight(1.5)
    simulation ! BoidsSimulationActor.PauseSimulation
    simulation ! BoidsSimulationActor.UpdateSeparationWeight(2.0)
    simulation ! BoidsSimulationActor.ResumeSimulation
    simulation ! BoidsSimulationActor.UpdateAlignmentWeight(0.8)
    simulation ! BoidsSimulationActor.TerminateSimulation
  }

  test("BoidsSimulation should handle multiple boid updates in sequence") {
    val simulation = testKit.spawn(BoidsSimulationActor())
    val boidRefs = (1 to 5).map(_ => testKit.createTestProbe[BoidActor.Command]().ref)

    simulation ! BoidsSimulationActor.StartSimulation

    // Simulate multiple boids updating velocities
    boidRefs.foreach(ref => simulation ! BoidsSimulationActor.BoidVelocityUpdated(ref))

    // Simulate multiple boids updating positions
    boidRefs.foreach(ref => simulation ! BoidsSimulationActor.BoidPositionUpdated(ref))
  }
