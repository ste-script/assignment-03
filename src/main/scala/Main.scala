import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

// "Actor" module definition
object HelloActor:
  // "API", i.e. message that actors should received / send
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])

  final case class Greeted(whom: String, from: ActorRef[Greet])

  // Behaviour factory, i.e how the actor react to messages
  def apply(): Behavior[Greet] = Behaviors.receive: (context, message) =>
    context.log.info("Hello {}!", message.whom)
    message.replyTo ! Greeted(message.whom, context.self)
    Behaviors.same

object HelloWorldAkkaTyped extends App:
  private val system: ActorSystem[HelloActor.Greet] = ActorSystem(HelloActor(), name = "hello-world")
  system ! HelloActor.Greet("Akka Typed", system.ignoreRef)
  Thread.sleep(5000)
  system.terminate()