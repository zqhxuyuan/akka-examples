package sample.calculator

import akka.actor._
import com.typesafe.config.ConfigFactory

object CalculatorRunner2 extends App {

  val remoteSystem = ActorSystem(Constant.ACTOR_SYSTEM_NAME, ConfigFactory.load("application"))

  scala.io.StdIn.readLine()

  remoteSystem.terminate()

}
