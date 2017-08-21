package concurrency.actor

import akka.actor._

case class Gamma(g: String)
case class Beta(b: String, g: Gamma)
case class Alpha(b1: Beta, b2: Beta)
case object Ping
case object Pong

class MyActor extends Actor {
  def receive = {
    // Literal String match
    case "Hello" =>
      println("Hi")
    // Literal Int match
    case 42 =>
      println("I don't know the question." +
        "Go ask the Earth Mark II.")
    // Matches any string at all
    case s: String =>
      println(s"You sent me a string: $s")
    // Match a more complex case class structure
    case Alpha(Beta(b1, Gamma(g1)), Beta(b2, Gamma(g2))) =>
      println(s"beta1: $b1, beta2: $b2, gamma1: $g1, gamma2: $g2")
    case Ping =>
      sender ! Pong
    // Catch all. Matches any message type
    case _ =>
  }
}

object MyActorApp {
  def main(args: Array[String]) {
    // We need a 'system' of Actors
    val system = ActorSystem("MyActors")
    // 'Props' gives us a way to modify certain aspects of an Actor's structure
    val actorProps = Props[MyActor]
    // And finally we pass the actor's props to the actorOf factory method
    val actor = system.actorOf(actorProps)

    import akka.pattern.ask
    import akka.util.Timeout
    import scala.concurrent.duration._
    // You must have a timeout for a Future, and we do this using an implicit value.
    implicit val askTimeout = Timeout(1.second)
    val question = actor ? 42
    println(question)
  }
}