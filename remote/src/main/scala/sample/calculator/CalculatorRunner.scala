package sample.calculator

import akka.actor._
import akka.pattern._
import sample.calculator.Messages._

import scala.concurrent.duration._

// 由于ArithmeticException默认的处理策略SupervisorStrategy是Restart，
// 一旦输入Div(0.0)时会重启将result清零。
// 我们可以在remote上加一个Supervisor来把异常处理策略改为Resume。
class SupervisorActor extends Actor {

  // 自定义异常处理
  def decider: PartialFunction[Throwable,SupervisorStrategy.Directive] = {
    case _: ArithmeticException => SupervisorStrategy.Resume
  }

  // 重载supervisor策略
  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds){
      decider.orElse(SupervisorStrategy.defaultDecider)
    }

  // supervisor actor会创建执行具体计算逻辑的calculator actor
  val calcActor = context.actorOf(CalcProps.props, Constant.CALCULATOR_ACTOR_NAME)

  // 所有发送到supervisor actor的消息,都会直接转发给calculator actor
  // supervisor actor的作用是重载了异常处理策略
  override def receive: Receive = {
    case msg@ _ => calcActor.forward(msg)
  }

}

object CalculatorRunner extends App {

  val remoteSystem = ActorSystem(Constant.ACTOR_SYSTEM_NAME)
  val calcActor = remoteSystem.actorOf(Props[SupervisorActor], Constant.CALCULATOR_SUPERACTOR_NAME)

  /*
  import remoteSystem.dispatcher

  calcActor ! Clear
  calcActor ! Num(13.0)
  calcActor ! Mul(1.5)

  implicit val timeout = akka.util.Timeout(1 second)

  ((calcActor ? GetResult).mapTo[String]) foreach println
  scala.io.StdIn.readLine()

  calcActor ! Div(0.0)
  calcActor ! Div(1.5)
  calcActor ! Add(100.0)
  ((calcActor ? GetResult).mapTo[String]) foreach println
  */

  scala.io.StdIn.readLine()

  remoteSystem.terminate()

}
