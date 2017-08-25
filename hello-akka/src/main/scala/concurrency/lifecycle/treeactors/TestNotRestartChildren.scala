package concurrency.lifecycle.treeactors

import akka.actor._

import scala.util.Random

/**
  * Created by zhengqh on 17/8/24.  AKKA CONCURRENCY PAGE-192
  *
  *
  */
object TestNotRestartChildren {

  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val parentActor = system.actorOf(Props[Parent3Actor], "parent")

    parentActor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }

}

class Parent3Actor extends Actor {
  val random = new Random()

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
      context.watch(context.actorOf(Props[Level13Actor], "child"))
    case msg@_ =>
      context.children.foreach(child => child forward msg)
      //throw new Exception("parent exception")
  }

  def initial(): Unit = {
    println("parent init")
  }

  override def preStart() {
    println("ParentActor pre start")
    context.watch(context.actorOf(Props[Level13Actor], "child"))
    //super.preStart()
  }
  /*
  override def postStop() = {
    println("ParentActor stopped")
    super.postStop()
  }
  */
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("ParentActor pre restart due to " + throwable.getMessage + ", failed msg:" + message)
    //super.preRestart(throwable, message)
    postStop()
  }
  override def postRestart(throwable: Throwable) = {
    println("ParentActor post restart")
    //super.postRestart(throwable)
    initial()
  }
}

class Level13Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stop" => self ! Kill
    case _ => throw new Exception("Level1Actor exception")
  }

  override def preStart() {
    println("Level1Actor pre start")
    context.watch(context.actorOf(Props[Level23Actor], "child_1"))
    super.preStart()
  }
  override def postStop() = {
    println("Level1Actor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level1Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Level1Actor post restart")
    super.postRestart(throwable)
  }
}

class Level23Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stop" => self ! Kill
    case _ => throw new Exception("Level2Actor exception")
  }

  override def preStart() {
    println("Level2Actor pre start")
    context.watch(context.actorOf(Props[Level33Actor], "child_1_1"))
    super.preStart()
  }
  override def postStop() = {
    println("Level2Actor stopped")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level2Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Level2Actor post restart")
    super.postRestart(throwable)
  }
}

class Level33Actor extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stop" => self ! Kill
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
    println("Level3Actor pre restart due to " + throwable.getMessage + ", failed msg:" + message)
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println("Level3Actor post restart")
    super.postRestart(throwable)
  }
}

