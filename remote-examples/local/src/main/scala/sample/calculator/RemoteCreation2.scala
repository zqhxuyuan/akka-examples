package sample.calculator

import akka.actor._
import akka.pattern._
import com.typesafe.config.ConfigFactory
import sample.calculator.Calculator._

import scala.concurrent.duration._

object RemoteCreation2 extends App {
  // load calculator.conf
  val localSystem = ActorSystem("localSystem", ConfigFactory.load("calculator"))

  // create SupervisorActor, notice the name should't be calculator, change to other name
  val calcActor = localSystem.actorOf(supervisorProps, name = "super-calculator")

  import localSystem.dispatcher

  calcActor ! Clear
  calcActor ! Num(13.0)
  calcActor ! Mul(1.5)

  implicit val timeout = akka.util.Timeout(1 second)

  ((calcActor ? GetResult).mapTo[String]) foreach println
  scala.io.StdIn.readLine()

  calcActor ! Div(0.0)
  calcActor ! Div(1.5)
  calcActor ! Add(100.0)
  ((calcActor ? GetResult).mapTo[String]) foreach println

  scala.io.StdIn.readLine()
  localSystem.terminate()

}