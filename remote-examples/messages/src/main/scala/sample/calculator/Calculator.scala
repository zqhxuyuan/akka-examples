package sample.calculator

import akka.actor._
import scala.concurrent.duration._

object Calculator {
  sealed trait MathOps
  case class Num(dnum: Double) extends MathOps
  case class Add(dnum: Double) extends MathOps
  case class Sub(dnum: Double) extends MathOps
  case class Mul(dnum: Double) extends MathOps
  case class Div(dnum: Double) extends MathOps

  sealed trait CalcOps
  case object Clear extends CalcOps
  case object GetResult extends CalcOps

  def props = Props(new Calculator)
  def supervisorProps = Props(new SupervisorActor)
}

class Calculator extends Actor with ActorLogging {
  import Calculator._

  var result: Double = 0.0   //internal state

  override def receive: Receive = {
    case Num(d) => result = d
    case Add(d) => result += d
    case Sub(d) => result -= d
    case Mul(d) => result *= d
    case Div(d) =>
      val _ = result.toInt / d.toInt   //yield ArithmeticException
      result /= d
    case Clear => result = 0.0
    case GetResult =>
      sender() ! s"Result of calculation is: $result"
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"Restarting calculator: ${reason.getMessage}")
    super.preRestart(reason, message)
  }
}

class SupervisorActor extends Actor {
  def decider: PartialFunction[Throwable,SupervisorStrategy.Directive] = {
    case _: ArithmeticException => SupervisorStrategy.Resume
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds){
      decider.orElse(SupervisorStrategy.defaultDecider)
    }

  // supervisor创建Calculator, 名称必须与calculator.conf的/calculator路径一致
  // 但如果改成其他名称,貌似也可以执行
  val calcActor = context.actorOf(Calculator.props,"calculator")

  override def receive: Receive = {
    case msg@ _ => calcActor.forward(msg)
  }

}
