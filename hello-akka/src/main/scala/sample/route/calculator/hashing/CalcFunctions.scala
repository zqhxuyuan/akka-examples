package sample.route.calculator.hashing

import akka.actor.{Actor, Props}
import Messages._

object CalcFunctions {
  def propsFuncs = Props(new CalcFunctions)
  def propsSuper = Props(new CalculatorSupervisor)
}

class CalcFunctions extends Actor {
  override def receive: Receive = {
    case Add(x,y) =>
      println(s"$x + $y carried out by ${self} with result=${x+y}")
    case Sub(x,y) =>
      println(s"$x - $y carried out by ${self} with result=${x - y}")
    case Mul(x,y) =>
      println(s"$x * $y carried out by ${self} with result=${x * y}")
    case Div(x,y) =>
      println(s"$x / $y carried out by ${self} with result=${x / y}")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    println(s"Restarting calculator: ${reason.getMessage}")
    super.preRestart(reason, message)
  }
}