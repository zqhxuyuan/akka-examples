package concurrency.lifecycle.childsupervisorwithoutwatch

import akka.actor._

class MyActorLifeCycleWithChild extends Actor {
//  override val supervisorStrategy = OneForOneStrategy() {
//    case _ => Stop
//  }

  // 1  8
  override def preStart() {
    // 在创建父亲之前, 先创建儿子, 即parent.preStart -> child.preStart
    val childActor = context.actorOf(Props[MyChildActor])
    println("my actor pre start and watch childActor: " + childActor)
    context.watch(childActor)
  }

  // 5
  override def postStop(): Unit = {
    println("my actor post stop..." + self)
    super.postStop()
  }

  // 4 重启parent时, 会先停止孩子, 然后才会调用parent自己的postStop
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("my actor pre restart due to " + message.get)
    super.preRestart(throwable, message)
  }

  // 注意: 孩子的preRestart()和postRestart()并没有调用

  // 7
  override def postRestart(throwable: Throwable) = {
    println("my actor post restart")
    super.postRestart(throwable)
  }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ =>
      throw new Exception("exception") // 3
  }
}

class MyChildActor extends Actor {
  def receive = {
    case _ => //throw new Exception("exception")
  }

  // 2  9
  override def preStart(): Unit = {
    println("child actor pre start...")
    super.preStart()
  }

  // 6
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

object MyActorLifeCycleWithChildApp {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycleWithChild")

    val actor = system.actorOf(Props[MyActorLifeCycleWithChild], "MyActorLifeCycleWithChild")

    // 3
    actor ! "suicide first time"

  }
}