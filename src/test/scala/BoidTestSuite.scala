import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import org.scalatest.funsuite.AnyFunSuite
import BoidMessages._

class BoidTestSuite extends AnyFunSuite:
  test("Boid should update position and velocity correctly") {
    // Create a test actor system
    val testBoid = BehaviorTestKit(BoidActor())
    val stateInbox = TestInbox[BoidState]()


    val boidPosition = (1.0, 2.0)
    val boidVelocity = (0.5, 0.5)
    //initial position and velocity of the boid
    testBoid.run(GetState(stateInbox.ref))
    val initialState = stateInbox.receiveMessage()
    println(
      s"Initial Boid state: position = ${initialState.position}, velocity = ${initialState.velocity}"
    )
    // Send a new message to the chatbox
    testBoid.run(UpdatePosition(boidPosition))
    // Verify the state of the boid
    testBoid.run(UpdateVelocity(boidVelocity))
    // Verify the state of the boid
    testBoid.run(GetState(stateInbox.ref))
    val state = stateInbox.receiveMessage()
    println(
      s"Boid state: position = ${state.position}, velocity = ${state.velocity}"
    )
    assert(state.position == boidPosition)
    assert(state.velocity == boidVelocity)

    // Request history
  }