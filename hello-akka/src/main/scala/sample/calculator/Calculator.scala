package sample.calculator

import akka.actor._
import akka.pattern._
import scala.concurrent.duration._

// 连续计算器, 本地模式运行
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
}

class Calculator extends Actor {

  import Calculator._

  var result: Double = 0.0 //internal state

  override def receive: Receive = {
    case Num(d) => result = d
    case Add(d) => result += d
    case Sub(d) => result -= d
    case Mul(d) => result *= d
    case Div(d) => result = result / d

    case Clear => result = 0.0
    case GetResult =>
      sender() ! s"Result of calculation is: $result"
  }
}

object CalculatorApp extends App {
  import Calculator._

  val _system = ActorSystem("calculator")
  val calculator = _system.actorOf(Props[Calculator], "calculator")

  calculator ! Num(1)
  calculator ! Add(1)
  calculator ! Mul(2)

  // 获取结果
  import _system.dispatcher
  implicit val timeout = akka.util.Timeout(1 second)

  ((calculator ? GetResult).mapTo[String]) foreach println

  _system.terminate()

}

