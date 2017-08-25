package sample.calculator

import akka.actor._
import sample.calculator.Messages._

object CalcProps {
  def props = Props(new Calcultor)
}

class Calcultor extends Actor with ActorLogging {

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