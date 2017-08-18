package sample.supervisor.backoff1

import akka.actor._
import akka.pattern._
import InnerChild.TestMessage

import scala.concurrent.duration._

object InnerChild {
  case class TestMessage(msg: String)
  class ChildException extends Exception

  def props = Props[InnerChild]
}

class InnerChild extends Actor with ActorLogging {
  import InnerChild._
  override def receive: Receive = {
    case TestMessage(msg) => //模拟子级功能
      log.info(s"Child received message: ${msg}")
  }
}

object Supervisor {

  def props: Props = { //在这里定义了监管策略和child Actor构建

    def decider: PartialFunction[Throwable, SupervisorStrategy.Directive] = {
      case _: InnerChild.ChildException => SupervisorStrategy.Restart
    }

    // create inner child
    val options = Backoff.onFailure(InnerChild.props, "innerChild", 1 second, 5 seconds, 0.0)
      .withManualReset
      .withSupervisorStrategy(
        OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds)(
          decider.orElse(SupervisorStrategy.defaultDecider)
        )
      )
    BackoffSupervisor.props(options)
  }
}

//注意：ParentalActor是Supervisor的父级，不是InnerChild的父级
object ParentalActor {
  case class SendToSupervisor(msg: InnerChild.TestMessage)
  case class SendToInnerChild(msg: InnerChild.TestMessage)
  case class SendToChildSelection(msg: InnerChild.TestMessage)
  def props = Props[ParentalActor]
}

class ParentalActor extends Actor with ActorLogging {
  import ParentalActor._

  //在这里构建子级Actor supervisor
  val supervisor = context.actorOf(Supervisor.props,"supervisor")

  supervisor ! BackoffSupervisor.getCurrentChild //要求supervisor返回当前子级Actor

  var innerChild: Option[ActorRef] = None   //返回的当前子级ActorRef

  val selectedChild = context.actorSelection("/user/parent/supervisor/innerChild")

  override def receive: Receive = {
    case BackoffSupervisor.CurrentChild(ref) =>   //收到子级Actor信息
      innerChild = ref
    case SendToSupervisor(msg) => supervisor ! msg
    case SendToChildSelection(msg) => selectedChild ! msg
    case SendToInnerChild(msg) => innerChild foreach(child => child ! msg)
  }
}

object BackoffSupervisorDemo1 extends App {
  import ParentalActor._
  val testSystem = ActorSystem("testSystem")
  val parent = testSystem.actorOf(ParentalActor.props,"parent")

  Thread.sleep(1000)   //wait for BackoffSupervisor.CurrentChild(ref) received

  parent ! SendToSupervisor(TestMessage("Hello message 1 to supervisor"))
  parent ! SendToInnerChild(TestMessage("Hello message 2 to innerChild"))
  parent ! SendToChildSelection(TestMessage("Hello message 3 to selectedChild"))

  scala.io.StdIn.readLine()

  testSystem.terminate()

}