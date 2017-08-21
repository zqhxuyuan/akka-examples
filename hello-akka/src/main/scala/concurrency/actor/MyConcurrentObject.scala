package concurrency.actor

import akka.actor.Actor.Receive
import akka.actor._

/**
  * Created by zhengqh on 17/8/18.
  */
case class DoSomething(withThis: String)

class MyConcurrentObject extends Actor{
  override def receive: Receive = {
    case DoSomething(withThis) =>
      print(withThis)
  }
}

object MyConcurrentApp {
  def main(args: Array[String]) {
    val actorSystem = ActorSystem("testActorSystem")
    val actor = actorSystem.actorOf(Props[MyConcurrentObject], "testActor")
    actor ! DoSomething("do some thing plz..")
    println("It's my turn now...")
  }
}

// 不能在普通对象中通过new创建Actor

object DemoManualCreateActor {
  def main(args: Array[String]) {
    // Exception in thread "main" akka.actor.ActorInitializationException:
    //   You cannot create an instance of [concurrency.actor.MyConcurrentObject]
    //   explicitly using the constructor (new).
    //   You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.
    val actorManual = new MyConcurrentObject
    println(actorManual)
  }
}

// 在Actor中可以通过new的方式创建子Actor

class ActorDemo(msg: String) extends Actor {
  override def receive: Actor.Receive = {
    case msg => println(msg)
  }
}

class ActorParent extends Actor {
  // It's ok to create sub-actor inside parent-actor with new ...
  val actor = context.actorOf(Props(new ActorDemo("test")), "parent")

  override def receive: Actor.Receive = {
    case msg => actor forward msg
  }
}

object DemoCreateActor {
  def main(args: Array[String]) {
    val actorSystem = ActorSystem("testActorSystem")
    val actor = actorSystem.actorOf(Props[ActorParent], "testActor")
    actor ! "haha"
    actorSystem.terminate()
  }
}