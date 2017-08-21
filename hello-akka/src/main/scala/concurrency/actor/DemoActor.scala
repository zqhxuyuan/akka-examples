package concurrency.actor

import akka.actor._

/**
  * Created by zhengqh on 17/8/18.
  */
abstract class AbstractActor {

  def forward (message: Any)(implicit context: ActorRef): Unit

  def ! (message: Any)(implicit sender: ActorRef = null): Unit

  def forward2(message: Any)(implicit context: ActorContext): Unit
}

class ImplActor extends AbstractActor {
  def forward(message: Any)(implicit context: ActorRef): Unit = {
    println(message)
  }

  def !(message: Any)(implicit sender: ActorRef): Unit = {
    println(message)
  }

  // tell is defined as
  def tell(msg: Any, sender: ActorRef): Unit = {
    println(msg)
  }

  def forward2(message: Any)(implicit context: ActorContext) =
    tell(message, ActorRef.noSender)

}

object ActorApp {
  def main(args: Array[String]) {

    implicit val self: ActorRef = null

    // Error:(40, 18) ambiguous implicit values: both value other of type akka.actor.ActorRef
    // and value self of type akka.actor.ActorRef match expected type akka.actor.ActorRef
    //implicit val other: ActorRef = null

    val actor = new ImplActor

    actor.forward("forward msg1")

    actor ! "tell msg"

    actor.tell("This is a message", Actor.noSender)

    implicit val context: ActorContext = null

    actor.forward2("forward msg2")
  }
}


