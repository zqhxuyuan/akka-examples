package sample.route.calculator.hashing

import akka.actor.{Actor, Props}
import akka.routing.FromConfig

object CalcRouter {
  def props = Props(new CalcRouter)
}

class CalcRouter extends Actor {

  // This router is used both with lookup and deploy of routees. If you
  // have a router with only lookup of routees you can use Props.empty
  // instead of Props[CalculatorSupervisor].
  // 只进行Routees查找，无需部署
  val calcRouter = context.actorOf(FromConfig.props(Props.empty), name = "calcRouter")

  override def receive: Receive = {
    case msg@ _ => calcRouter forward msg
  }
}