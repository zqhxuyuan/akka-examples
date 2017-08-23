package concurrency.lifecycle.childsupervisorwithoutwatch

import akka.actor.SupervisorStrategy._
import akka.actor._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * 父类监管子类, 当子类抛出异常时, 父类的supervisorStrategy会起作用
  *
  * 策略为AllForOneStrategy, Decider为Stop时
  *
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child1#-1729977252]
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child2#129304096]
    Child1Actor pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1245671373]
    java.lang.Exception: Child1Actor exception

    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child2#129304096]
    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child1#-1729977252]
    Child1Actor post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-1245671373]
  *
  */
object TestSupervisorChildExceptionAllForOne {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent1Actor], "parent")

    actor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent1Actor extends Actor {
  // 监控策略定义在父类中, 表示父类如何监管子类
  // AllForOne表示: 子类抛出异常, 父类处理所有的子类, 而不仅仅是抛出异常的子类
  // OneForOne表示: 子类抛出异常, 父类只处理这个出现异常的子类
  override val supervisorStrategy =
    AllForOneStrategy(5, 1 minute) {
      case _ => Stop // Stop表示只要子类抛出异常, 父类就直接杀掉子类
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






