import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox}
import org.scalatest.funsuite.AnyFunSuite
import BoidMessages._

class BoidActorTest extends AnyFunSuite:
  test("Boid should update position and velocity correctly") {
    // Create a test actor system
    val testBoid = BehaviorTestKit(BoidActor())


    val boidPosition = (1.0, 2.0)
    val boidVelocity = (0.5, 0.5)
    // Send a message to update the position of the boid

    // Send a new message to the chatbox
    testBoid.run(UpdatePosition(boidPosition))

    // Request history
  }