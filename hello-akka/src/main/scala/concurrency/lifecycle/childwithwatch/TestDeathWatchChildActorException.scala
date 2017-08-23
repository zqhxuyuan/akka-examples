package concurrency.lifecycle.childwithwatch

import akka.actor.SupervisorStrategy._
import akka.actor._

import scala.concurrent.duration._
import scala.util.Random

/**
  * 采用DeathWatch有两个步骤:
  *
  * 1. 创建子类时, 加上context.watch(childActor)
  * 2. 在父类中定义一个Terminated的处理
  *
  * 注意, 这个测试类中虽然添加了death watch, 但默认子类抛出的Exception, 监控策略的Decider为Restart
  *
    child1 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child1#539604897]
    child3 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-649165155]
    child2 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child2#-1280670704]

    child1 pre restart due to child1 exception, receive msg: Some(stop) @@Actor[akka://MyActorLifeCycle/user/parent/child1#539604897]
    child1 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child1#539604897]

    java.lang.Exception: child1 exception

    child1 post restart @@Actor[akka://MyActorLifeCycle/user/parent/child1#539604897]
    child1 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child1#539604897]

  terminate system时

    child1 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child1#539604897]
    child3 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-649165155]
    child2 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child2#-1280670704]

  这里并没有打印出... has died

  TODO Question.
  为什么终止系统的时候, 会调用Actor的post stop方法, 但不会发送Terminated消息?

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
    case "stopchild" =>
      // 随机发送一条消息给子类, 所有的子类处理任意消息都会模拟抛出一个异常
      val children = context.children.toList
      val index = random.nextInt(2)
      val randomChild = children(index)
      randomChild ! "stop"
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

