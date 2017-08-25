package concurrency.lifecycle.multiactors

import akka.actor.SupervisorStrategy._
import akka.actor.{OneForOneStrategy, _}

// deal exception to watching actor
object TestMultiActorsLifeCycleWatchingException {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActors2")

    // otherActor and myActor is not parent-child relationship, but in the same level
    // 先创建OtherActor,然后把OtherActor的引用传递给MyActor
    // MyActor在preStart中会监控OtherActor
    val otherActor = system.actorOf(Props[Actor1], "OtherActor")
    val actor = system.actorOf(Props(new Actor2(otherActor)), "MyActor")

    // 3 模拟Exception的默认监管策略, 重启UserGuardian Actor
    // 注意: MyActor是OtherActor的watcher, 但不是parent
    actor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }
}

// kill watching actor
object TestMultiActorsLifeCycleWatchingKill {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActors2")
    val otherActor = system.actorOf(Props[Actor1], "OtherActor")
    val actor = system.actorOf(Props(new Actor2(otherActor)), "MyActor")

    actor ! "kill"

    Thread.sleep(5000)
    system.terminate()
  }
}

// kill watched actor
object TestMultiActorsLifeCycleWatchedKill {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActors2")

    val otherActor = system.actorOf(Props[Actor1], "OtherActor")
    val actor = system.actorOf(Props(new Actor2(otherActor)), "MyActor")

    // kill第一个Actor, 由于第二个Actor监控了第一个Actor, 第二个Actor会收到Terminated消息
    otherActor ! "kill"

    Thread.sleep(5000)
    system.terminate()
  }
}

// deal exception to watched actor
object TestMultiActorsLifeCycleWatchedException {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActors2")

    val otherActor = system.actorOf(Props[Actor1], "OtherActor")
    val actor = system.actorOf(Props(new Actor2(otherActor)), "MyActor")

    otherActor ! "Exception"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Actor2(otherActor: ActorRef) extends Actor {
  // 由于Actor2不是Actor1的父类, 所以定义supervisor没有意义
  // 对于Actor1和Actor2而言, 由于它们都是一个User Guardian
  // 如果要改变它们的监控策略, 需要自定义UserGuardian...
//  override val supervisorStrategy = OneForOneStrategy() {
//    case _ => Stop
//  }

  // 2  7
  override def preStart() {
    println("my actor pre start...")
    context.watch(otherActor)
    super.preStart()
  }

  // 5  9
  override def postStop(): Unit = {
    println("my actor post stop...")
    super.postStop()
  }

  // 4
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("my actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }

  // 6
  override def postRestart(throwable: Throwable) = {
    println("my actor post restart")
    super.postRestart(throwable)
  }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "kill" =>
      self ! Kill
    case _ =>
      throw new Exception("exception") // 3
  }
}

class Actor1 extends Actor {
  def receive = {
    case "kill" => self ! Kill
    case _ => throw new Exception("other actor exception")
  }

  // 1
  override def preStart(): Unit = {
    println("other actor pre start...")
    super.preStart()
  }

  // 8
  override def postStop() = {
    println("other actor post stop...")
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("other actor pre restart")
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("other actor post restart")
    super.postRestart(throwable)
  }
}

