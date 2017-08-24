package concurrency.lifecycle.treeactors

import akka.actor._
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy._
import scala.util.Random

/**
  */
object TestTreeActorsSupervisor {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val parentActor = system.actorOf(Props[Parent2Actor], "parent")

    parentActor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent2Actor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(5, 1 minute) {
      case _ => Restart
    }

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
    context.watch(context.actorOf(Props[Level11Actor], "child"))
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

class Level11Actor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(5, 1 minute) {
      case _ => Restart
    }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ => throw new Exception("Level1Actor exception")
  }

  override def preStart() {
    println("Level1Actor pre start")
    context.watch(context.actorOf(Props[Level21Actor], "child_1"))
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

class Level21Actor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(5, 1 minute) {
      case _ => Restart
    }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ => throw new Exception("Level2Actor exception")
  }

  override def preStart() {
    println("Level2Actor pre start")
    context.watch(context.actorOf(Props[Level31Actor], "child_1_1"))
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


class Level31Actor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(5, 1 minute) {
      case _ => Restart
    }

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
