package concurrency.lifecycle.multiactors

import akka.actor.Actor.Receive
import akka.actor._

/**
  * Created by zhengqh on 17/8/24.
  */
object TestErrorCreationActor {

  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val parentActor = system.actorOf(Props[ParentActor], "parent")

    parentActor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }

}

class ParentActor extends Actor {
  def receive = {
    case _ =>
  }

  override def preStart(): Unit = {
    context.actorOf(Props[ChildActor], "child1")
  }
}

class ChildActor extends Actor {
  def receive = {
    case _ =>
  }

  override def preStart(): Unit = {
    // DON'T DO THIS WAY
    //context.actorOf(Props[ChildActor], "child2")

    // ALSO DON'T THIS
    //context.actorOf(Props[ParentActor], "child2")
  }
}
