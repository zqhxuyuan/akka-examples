package sample.cluster.calculator.frontend

import akka.actor._
import com.typesafe.config.ConfigFactory

object FrontEnd {
  private var router: ActorRef = _
  def create = {  //must load this seed-node before any backends
    val calcSystem = ActorSystem("calcClusterSystem",ConfigFactory.load("calculator").getConfig("Frontend"))
    router = calcSystem.actorOf(CalcRouter.props,"frontend")
  }
  def getRouter = router
}