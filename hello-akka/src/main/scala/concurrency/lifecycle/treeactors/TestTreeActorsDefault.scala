package concurrency.lifecycle.treeactors

import akka.actor._

import scala.util.Random

/**
  * 创建层级Actor时, 从父亲到孩子,再到孩子的孩子, 依次调用preStart方法
  *
    Parent Actor pre start
    Level1Actor pre start
    Level2Actor pre start
    Level3Actor pre start

  Parent向第一层的孩子(Level1)发送消息, 而Level1处理时抛出异常
  默认Level1父亲的监管策略是重启孩子(Level1). 注意: 没有重启Parent

    Level1Actor pre restart due to Level1Actor exception, failed msg:stop
    [ERROR] [08/24/2017 09:40:04.624] [MyActorLifeCycle-akka.actor.default-dispatcher-4] [akka://MyActorLifeCycle/user/parent/child] Level1Actor exception
    java.lang.Exception: Level1Actor exception

  重启Level1时,会先停止所有的孩子Level2
  停止Level2时,也会停止Level2的孩子Level3

    Level1Actor stopped
    Level3Actor stopped
    Level2Actor stopped

  Level1重启完毕后, 会调用Level1的postRestart方法. 但是并不会调用Level2和Level3的postRestart
  所以: 只有Level1才会重启, Level2和Level3并没有重启

    Level1Actor post restart

  接着调用Level1 -> Level2 -> Level3的preStart方法

    Level1Actor pre start
    Level2Actor pre start
    Level3Actor pre start


    Level3Actor stopped
    Level2Actor stopped
    Level1Actor stopped
    Parent Actor stopped

  */
object TestTreeActorsDefault {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val parentActor = system.actorOf(Props[Parent1Actor], "parent")

    parentActor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent1Actor extends Actor {
  val random = new Random()

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stopchild" =>
      // kill first level child
      context.children.foreach(_ ! "stop")
  }

  override def preStart() {
    println("Parent Actor pre start")
    context.watch(context.actorOf(Props[Level1Actor], "child"))
    super.preStart()
  }
  override def postStop() = {
    println("Parent Actor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Parent Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Parent Actor post restart")
    super.postRestart(throwable)
  }
}

class Level1Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ => throw new Exception("Level1Actor exception")
  }

  override def preStart() {
    println("Level1Actor pre start")
    context.watch(context.actorOf(Props[Level2Actor], "child_1"))
    super.preStart()
  }
  override def postStop() = {
    println("Level1Actor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level1Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Level1Actor post restart")
    super.postRestart(throwable)
  }
}

class Level2Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ => throw new Exception("Level2Actor exception")
  }

  override def preStart() {
    println("Level2Actor pre start")
    context.watch(context.actorOf(Props[Level3Actor], "child_1_1"))
    super.preStart()
  }
  override def postStop() = {
    println("Level2Actor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level2Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Level2Actor post restart")
    super.postRestart(throwable)
  }
}


class Level3Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ => throw new Exception("Level3Actor exception")
  }

  override def preStart() {
    println("Level3Actor pre start")
    super.preStart()
  }
  override def postStop() = {
    println("Level3Actor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level3Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Level3Actor post restart")
    super.postRestart(throwable)
  }
}
