package sample.route.calculator.adaptive

import akka.actor._
import com.typesafe.config.ConfigFactory

object BackEnd {
  def create(port: Int): Unit = {   //create instances of backend Calculator
    val config = ConfigFactory.parseString("akka.cluster.roles = [backend]")
      .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port"))
      .withFallback(ConfigFactory.load("calculator2"))
    val calcSystem = ActorSystem("calcClusterSystem",config)
    val calcRef = calcSystem.actorOf(CalcFunctions.propsSuper,"calculator")
  }
}