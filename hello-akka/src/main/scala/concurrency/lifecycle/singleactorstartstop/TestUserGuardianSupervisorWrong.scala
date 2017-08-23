package concurrency.lifecycle.singleactorstartstop

import akka.actor._
import akka.actor.SupervisorStrategy._

/**
  * 修改监控策略: 改为Stop. 不过由于该Actor是UserGuardian的child
  * 在Actor中直接定义supervisorStrategy是不起作用的. 即仍然会有restart
  */
object TestUserGuardianSupervisorWrong {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[MyActor3], "MyActorLifeCycle")

    actor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }
}

class MyActor3 extends Actor {
  // HERE
  override val supervisorStrategy = OneForOneStrategy() {
    case _ => Stop // THIS STRATEGY NOT WORK TO MyActor
  }

  override def preStart() {
    println("my actor pre start...")
    super.preStart()
  }

  override def postStop(): Unit = {
    println("my actor post stop...")
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
    case "exception" =>
      throw new Exception("exception")
    case _ =>
  }
}
