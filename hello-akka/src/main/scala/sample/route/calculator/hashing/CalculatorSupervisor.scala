package sample.route.calculator.hashing

import akka.actor._

import scala.concurrent.duration._

class CalculatorSupervisor extends Actor {
  def decider: PartialFunction[Throwable,SupervisorStrategy.Directive] = {
    case _: ArithmeticException => SupervisorStrategy.Resume
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds){
      decider.orElse(SupervisorStrategy.defaultDecider)
    }

  val calcActor = context.actorOf(CalcFunctions.propsFuncs,"calcFunction")

  override def receive: Receive = {
    case msg@ _ => calcActor.forward(msg)
  }
}