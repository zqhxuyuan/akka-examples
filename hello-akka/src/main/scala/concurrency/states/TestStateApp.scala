package concurrency.states

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask

/**
  * Created by zhengqh on 17/8/27.
  */
object TestStateApp {
  def main(args: Array[String]) {
    val actorSystem = ActorSystem("Test")
    val actor = actorSystem.actorOf(Props[MyActor])
    import MyActor._

    implicit val timeout = Timeout(5.seconds) // needed for '?' below
    //implicit val dispatcher = actorSystem.dispatcher

    // prints: Huh? Who are you?
    println(Await.result(actor ? Goodbye("So long"), 1.second))

    // prints: Hello(Hithere to you too!)
    println(Await.result(actor ? Hello("Hithere"), 1.second))

    // prints: We've already done that.
    println(Await.result(actor ? Hello("Hithere again"), 1.second))

    // prints: Goodbye(So long, dood!)
    println(Await.result(actor ? Goodbye("So long"), 1.second))

    actorSystem.terminate()
  }
}

class MyActor extends Actor {
  import MyActor._

  // expect receive hello message
  // But If we receive Goodbye, we'll simply return another message
  def expectHello: Receive = {
    case Hello(greeting) =>
      sender ! Hello(greeting + " to you too!")

      context.become(expectGoodbye)
    case Goodbye(_) =>
      sender ! "Huh? Who are you?"
  }

  // expect receive goodbye message
  def expectGoodbye: Receive = {
    case Hello(_) =>
      sender ! "We've already done that."
    case Goodbye(_) =>
      sender ! Goodbye("So long, dood!")

      context.become(expectHello)
  }

  def receive = expectHello
}

object MyActor {
  case class Hello(msg: String)
  case class Goodbye(msg: String)
}
