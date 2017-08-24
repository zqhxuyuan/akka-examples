package concurrency.lifecycle.userguardian

import akka.actor.SupervisorStrategy._
import akka.actor._

/**
  * 自定义SupervisorStrategyConfigurator, 并且在配置文件中配置:
  * guardian-supervisor-strategy.
  *
  * Restart策略的输出结果示例:
  *
  * 1. my actor pre start...
  * 2. my actor pre restart due to exception
  * 3. throw java.lang.Exception: exception
  * 4. my actor post stop...
  * 5. my actor post restart
  * 6. my actor pre start...
  * 7. my actor post stop...
  *
  * Stop策略的输出结果
  *
  * 1. my actor pre start...
  * 2. throw java.lang.Exception: exception
  * 3. my actor post stop...
  *
  * Resume策略的输出结果
  *
  * 1. my actor pre start...
  * 2. my actor post stop...
  */
object TestUserGuardianSupervisor {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[MyActor4], "MyActorLifeCycle")

    actor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }
}

class MyActor4 extends Actor {
  // NO NEED HERE
//  override val supervisorStrategy = OneForOneStrategy() {
//    case _ => Stop // THIS STRATEGY NOT WORK TO MyActor
//  }

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

class UserGuardianStrategyConfigurator extends SupervisorStrategyConfigurator {
  def create(): SupervisorStrategy = {
    OneForOneStrategy() {
      case _ => Resume
    }
  }
}
