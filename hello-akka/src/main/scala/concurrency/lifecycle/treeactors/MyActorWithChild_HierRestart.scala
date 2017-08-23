package concurrency.lifecycle.treeactors

import akka.actor._

import scala.util.Random

class MyActorWithChild_HierRestart extends Actor {
//  override val supervisorStrategy =
//    OneForOneStrategy(5, 1 minute) {
//      case _ => Restart
//    }

  override def preStart() {
    println("Parent Actor pre start")
    context.watch(context.actorOf(Props[Level1Actor], "child"))
    super.preStart()
  }

  val random = new Random()

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stopchild" =>
      context.children.foreach(_ ! "stop")
  }
}

class Level1Actor extends Actor {
//  override val supervisorStrategy =
//    OneForOneStrategy(5, 1 minute) {
//      case _ => Restart
//    }

  override def preStart() {
    println("Level1Actor pre start")
    context.watch(context.actorOf(Props[Level2Actor], "child_1"))
    super.preStart()
  }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ => throw new Exception("Level1Actor exception")
  }

  override def postStop() = {
    println("Level1Actor stopped")
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level1Actor actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("Level1Actor actor post restart")
    super.postRestart(throwable)
  }
}

class Level2Actor extends Actor {
//  override val supervisorStrategy =
//    OneForOneStrategy(5, 1 minute) {
//      case _ => Restart
//    }

  override def preStart() {
    println("Level2Actor pre start")
    context.watch(context.actorOf(Props[Level3Actor], "child_1_1"))
    super.preStart()
  }

  def receive = {
    case _ => throw new Exception("Level2Actor exception")
  }

  override def postStop() = {
    println("Level2Actor stopped")
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level2Actor actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("Level2Actor actor post restart")
    super.postRestart(throwable)
  }
}


class Level3Actor extends Actor {
//  override val supervisorStrategy =
//    OneForOneStrategy(5, 1 minute) {
//      case _ => Restart
//    }

  override def preStart() {
    println("Level3Actor pre start")
    super.preStart()
  }

  def receive = {
    case _ => throw new Exception("Level3Actor exception")
  }

  override def postStop() = {
    println("Level3Actor stopped")
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("Level3Actor actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("Level3Actor actor post restart")
    super.postRestart(throwable)
  }
}

object MyActorWithChild_HierRestartApp {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[MyActorWithChild_HierRestart], "parent")

    actor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}