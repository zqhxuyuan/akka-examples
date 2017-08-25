package sample.cluster.calculator.backend

import akka.actor._
import com.typesafe.config.ConfigFactory

// Backend factory method, Backend actor is CalculatorSupervisor which create calculator
object Calculator {
  def create(role: String): Unit = {   //create instances of backend Calculator

    val config = ConfigFactory.parseString("Backend.akka.cluster.roles = [\""+role+"\"]")
      .withFallback(ConfigFactory.load("calculator")).getConfig("Backend")

    val calcSystem = ActorSystem("calcClusterSystem",config)

    // each calculator role has an supervisor, the supervisor is respond to create calculator sub-actor
    val calcRef = calcSystem.actorOf(CalcFunctions.propsSuper(role),"calculator")
  }
}