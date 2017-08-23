package concurrency.lifecycle.singleactorstartstop

import akka.actor.{Props, ActorSystem, Actor}

/**
  * Actor Life Cycle
  *
  * 1. pre start hook
  * 2. create actor action
  * 3. destroy actor action
  * 4. post stop hook
  *
  */
object TestStartStop {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[MyActor1], "MyActorLifeCycle")
    Thread.sleep(5000)
    system.terminate()
  }
}

class MyActor1 extends Actor {
  override def preStart() {
    println("my actor pre start...")
    super.preStart()
  }

  override def postStop(): Unit = {
    println("my actor post stop...")
    super.postStop()
  }

  // 下面两个方法并不会被调用
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("my actor pre restart due to " + message.get)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("my actor post restart")
    super.postRestart(throwable)
  }

  def receive = {
    case _ =>
  }
}
