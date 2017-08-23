package concurrency.lifecycle.childsupervisorwithoutwatch

import akka.actor.SupervisorStrategy._
import akka.actor._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * 父类监管子类, 当子类抛出异常时, 父类的supervisorStrategy会起作用
  *
  * 策略为OneForOneStrategy, Decider为Restart时
  *
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1628268025]
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child1#-1013995347]
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child2#964743101]

  child3 restart

    Child1Actor pre restart due to Child1Actor exception, receive msg: Some(stop) @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1628268025]
    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1628268025]

    java.lang.Exception: Child1Actor exception
    Child1Actor post restart @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1628268025]
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1628268025]


  when terminate

    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child2#964743101]
    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1628268025]
    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child1#-1013995347]

  TODO Question:

  重启Actor(这里是child3), 为什么Actor的引用没有变化? 这里都是child3#-1628268025
  */
object TestSupervisorChildExceptionRestart {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent3Actor], "parent")

    actor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent3Actor extends Actor {
  override val supervisorStrategy =
    OneForOneStrategy(5, 1 minute) {
      case _ => Restart
    }

  override def preStart() {
    // 模拟创建多个子类, 子类的类型可以不同, 也可以相同
    context.actorOf(Props(new Child1Actor("child1")), "child1")
    context.actorOf(Props(new Child1Actor("child2")), "child2")
    context.actorOf(Props(new Child1Actor("child3")), "child3")
  }

  val random = new Random()

  def receive = {
    case "stopchild" =>
      // 随机发送一条消息给子类, 所有的子类处理任意消息都会模拟抛出一个异常
      val children = context.children.toList
      val index = random.nextInt(3)
      val randomChild = children(index)
      randomChild ! "stop"
  }
}


