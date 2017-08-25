package concurrency.lifecycle.treeactors

import akka.actor._

import scala.util.Random

/**
  *
  * Actor树形结构:

  * Parent
  * |
  * Level1
  * |
  * Level2
  * |
  * Level3

  * 创建层级Actor时, 从父亲到孩子,再到孩子的孩子, 依次调用preStart方法

  * ParentActor pre start
  * Level1Actor pre start
  * Level2Actor pre start
  * Level3Actor pre start

  * Parent向第一层的孩子(Level1)发送消息, 而Level1处理时抛出异常
  * 默认Level1父亲的监管策略是重启孩子, 即调用Level1的preRestart方法

  * Level1Actor pre restart due to Level1Actor exception, failed msg:stop
  * Level1Actor stopped
  * [ERROR] [08/24/2017 09:40:04.624] [MyActorLifeCycle-akka.actor.default-dispatcher-4] [akka://MyActorLifeCycle/user/parent/child] Level1Actor exception
  * java.lang.Exception: Level1Actor exception

  * preRestart方法的默认实现是: 停止所有的孩子, 然后调用自己的postStop方法.  停止是一个级联操作

  * 重启Level1时,会先停止Level1的所有孩子,即Level2
  * 停止Level2时,也会停止Level2的所有孩子,即Level3

  * Level3Actor stopped
  * Level2Actor stopped

  * 问题: 为什么stop children的顺序是Level1, Level3, Level2?
  * 1. context.stop(child)是一个异步调用
  * 2. stop是个级联操作, Level2的孩子是Level3, 所以先stop level3, 然后stop level2
  * 3. 那么为什么Level1最先呢, 因为preRestart最后还会调用postStop

  * Level1重启完毕后, 会调用Level1的postRestart方法. 但是并不会调用Level2和Level3的postRestart
  * 所以: 只有Level1才会重启, Level2和Level3并没有重启. 只有重启才会依次调用preRestart和postRestart

  * Level1Actor post restart

  * 接着调用Level1 -> Level2 -> Level3的preStart方法

  * Level1Actor pre start
  * Level2Actor pre start
  * Level3Actor pre start

  * terminate系统时

  * Level3Actor stopped
  * Level2Actor stopped
  * Level1Actor stopped
  * ParentActor stopped

  * COPY FROM AKKA-CONCURRENCY PAGE-187
  * >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
  * The default implementation of actor restart involves the following steps:

  * 1. Suspend the actor’s processing.

  * 2. Suspend the processing of all of the actor’s children.

  * 3. Call preRestart on the failed actor instance.
  * • Terminates all children. This is not a blocking operation, but Akka does guarantee that your actor won’t start up again until all its children have stopped.
  * • Calls postStop on the failed actor instance.

  * 4. Constructs a new instance of the failed actor using the originally provided factory method.
  * • Note that this is a standard construction of your actor code.
  * • If you’re creating children in your constructor, then they will get created at this time.

  * 5. The postRestart method will be called on this new instance.
  * • If you’re creating children in your preStart method, then they will get created at this time.

  * 6. The new actor is then put back on the queue to process incoming messages.
  */
object TestTreeActorsDefault {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val parentActor = system.actorOf(Props[Parent1Actor], "parent")

    parentActor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent1Actor extends Actor {
  val random = new Random()

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case msg@_ =>
      context.children.foreach(child => child forward msg)
  }

  override def preStart() {
    println("ParentActor pre start")
    context.watch(context.actorOf(Props[Level1Actor], "child"))
    super.preStart()
  }
  override def postStop() = {
    println("ParentActor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("ParentActor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("ParentActor post restart")
    super.postRestart(throwable)
  }
}

class Level1Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stop" => self ! Kill
    case _ => throw new Exception("Level1Actor exception")
  }

  override def preStart() {
    println("Level1Actor pre start @@" + self)
    context.watch(context.actorOf(Props[Level2Actor], "child_1"))
    super.preStart()
  }
  override def postStop() = {
    println("Level1Actor stopped @@" + self)
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
    case "stop" => self ! Kill
    case _ => throw new Exception("Level2Actor exception")
  }

  override def preStart() {
    println("Level2Actor pre start @@" + self)
    context.watch(context.actorOf(Props[Level3Actor], "child_1_1"))
    super.preStart()
  }
  override def postStop() = {
    println("Level2Actor stopped @@" + self)
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
    case "stop" => self ! Kill
    case _ => throw new Exception("Level3Actor exception")
  }

  override def preStart() {
    println("Level3Actor pre start @@" + self)
    super.preStart()
  }
  override def postStop() = {
    println("Level3Actor stopped @@" + self)
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
