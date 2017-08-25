package concurrency.lifecycle.watchandsupervisor

import akka.actor.SupervisorStrategy._
import akka.actor.{OneForOneStrategy, _}
import scala.concurrent.duration._

object TestWatchChildAndSupervisor {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycleWithChild")

    val actor = system.actorOf(Props[ParentActor], "parent")

    actor ! "parent msg"
    actor ! "parent msg"
    actor ! "parent msg"

    Thread.sleep(5000)
    system.terminate()
  }
}

class ParentActor extends Actor {
  override val supervisorStrategy = OneForOneStrategy(2, 1.minutes) {
    case _ => Restart // TODO try change to Stop and watch what would happen
  }

  override def preStart() {
    // 在创建父亲之前, 先创建儿子, 即parent.preStart -> child.preStart
    val childActor = context.actorOf(Props[ChildActor], "child")
    println("my actor pre start and watch childActor: " + childActor)
    context.watch(childActor)
  }

  override def postStop(): Unit = {
    println("my actor post stop..." + self)
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("my actor pre restart due to " + message.get)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("my actor post restart")
    super.postRestart(throwable)
  }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case msg@_ =>
      context.children.foreach(child => child forward msg)
  }
}

class ChildActor extends Actor {
  def receive = {
    case _ => throw new Exception("child exception")
  }

  override def preStart(): Unit = {
    println("child actor pre start...")
    super.preStart()
  }

  override def postStop() = {
    println("child actor post stop..." + self)
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("child actor pre restart due to " + message.get)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("child actor post restart")
    super.postRestart(throwable)
  }
}