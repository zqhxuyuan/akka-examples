package concurrency.lifecycle.deathwatch

import akka.actor.SupervisorStrategy._
import akka.actor._

import scala.concurrent.duration._
import scala.util.Random

/**
  * 采用DeathWatch有两个步骤:
  *
  * 1. preStart时加上context.watch(actor)
  * 2. 在父类中定义一个Terminated的处理
  */
object TestDeathWatchChildActorException {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent1Actor], "parent")

    actor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}

object TestDeathWatchChildActorKill {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent1Actor], "parent")

    actor ! "kill"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent1Actor extends Actor {
  override def preStart() {
    context.watch(context.actorOf(Props(new Child1Actor("child1")), "child1"))
    context.watch(context.actorOf(Props(new Child1Actor("child2")), "child2"))
    context.watch(context.actorOf(Props(new Child1Actor("child3")), "child3"))
  }

  val random = new Random()

  def receive = {
    // 当子类被停止后, 它会发送Terminated消息给父类处理
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case msg@_ =>
      // 随机发送一条消息给子类, 所有的子类处理任意消息都会模拟抛出一个异常
      val children = context.children.toList
      val index = random.nextInt(2)
      val randomChild = children(index)
      randomChild ! msg
  }
}

class Child1Actor(actorName: String = "child") extends Actor {
  def receive = {
    case "kill" =>
      self ! Kill
    case _ =>
      throw new Exception(s"$actorName exception")
  }

  override def preStart() {
    println(s"$actorName pre start..." + " @@" + self)
    super.preStart()
  }

  override def postStop(): Unit = {
    println(s"$actorName post stop..." + " @@" + self)
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println(s"$actorName pre restart due to " + throwable.getMessage +
      ", receive msg: " + message + " @@" + self)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println(s"$actorName post restart" + " @@" + self)
    super.postRestart(throwable)
  }
}

