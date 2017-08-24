package concurrency.lifecycle.childsupervisorwithoutwatch

import akka.actor.SupervisorStrategy._
import akka.actor._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * 父类监管子类, 当子类抛出异常时, 父类的supervisorStrategy会起作用
  *
  * 策略为OneForOneStrategy, Decider为Resume时
  *
  */
object TestSupervisorChildExceptionResume {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent4Actor], "parent")

    actor ! "stopchild"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent4Actor extends Actor {
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


