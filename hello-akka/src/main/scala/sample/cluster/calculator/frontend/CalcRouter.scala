package sample.cluster.calculator.frontend

import akka.actor._
import sample.cluster.calculator.Messages._

object CalcRouter {
  def props = Props(new CalcRouter)
}

// 路由器(Router)
class CalcRouter extends Actor {
  // 路由者(Routee)
  var nodes: Map[String,ActorRef] = Map()

  override def receive: Receive = {
    // 收集到BackEnd(Calculator)的注册事件,将BackEnd加入到Routees列表
    case RegisterBackendActor(role) =>
      nodes += (role -> sender())
      // 监视Routee
      context.watch(sender())
    case add: Add => routeCommand("adder", add)
    case sub: Sub => routeCommand("substractor",sub)
    case mul: Mul => routeCommand("multiplier",mul)
    case div: Div => routeCommand("divider",div)

    // 如果Routee挂掉,从Routees列表中移除
    case Terminated(ref) =>    //remove from register
      nodes = nodes.filter { case (_,r) => r != ref}
  }

  // 对不同的指令路由到对应的Routee
  // 每个指定都对应一个Routee
  def routeCommand(role: String, ops: MathOps): Unit = {
    nodes.get(role) match {
      case Some(ref) => ref ! ops
      case None =>
        println(s"$role not registered!")
    }
  }
}

