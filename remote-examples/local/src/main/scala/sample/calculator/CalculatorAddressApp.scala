package sample.calculator

import akka.actor._
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern._
import sample.calculator.Messages._

/**
  * Created by zhengqh on 17/8/14.
  */
object CalculatorAddressApp extends App{

  val localSystem = ActorSystem("localSystem")
  val path = s"akka.tcp://${Constant.ACTOR_SYSTEM_NAME}@127.0.0.1:2552/user/${Constant.CALCULATOR_SUPERACTOR_NAME}/${Constant.CALCULATOR_ACTOR_NAME}"

  import localSystem.dispatcher
  implicit val timeout = Timeout(5 seconds)

  // Akka-Remoting提供了两种远程查找方式：actorSelection.resolveOne方法和Identify消息确认

  val remoteActor = localSystem.actorSelection(path)
  val lookupActor = remoteActor.resolveOne()

  for (calcActor : ActorRef <- lookupActor) {
    calcActor ! Clear
    calcActor ! Num(13.0)
    calcActor ! Mul(1.5)
    ((calcActor ? GetResult).mapTo[String]) foreach println

    calcActor ! Div(0.0)
    calcActor ! Div(1.5)
    calcActor ! Add(100.0)
    ((calcActor ? GetResult).mapTo[String]) foreach println
  }

  scala.io.StdIn.readLine()
  localSystem.terminate()
}
