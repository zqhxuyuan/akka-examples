package sample.cluster.calculator.backend

import akka.actor.{Actor, Props}
import sample.cluster.calculator.Messages.{Add, Div, Mul, Sub}

object CalcFunctions {
  def propsFuncs = Props(new CalcFunctions)
  def propsSuper(role: String) = Props(new CalculatorSupervisor(role))
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
