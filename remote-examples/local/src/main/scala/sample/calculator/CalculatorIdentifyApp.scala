package sample.calculator

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import sample.calculator.Messages._

import scala.concurrent.duration._

/**
  * Created by zhengqh on 17/8/14.
  */
object CalculatorIdentifyApp extends App{
  val localSystem = ActorSystem("localSystem")
  val localActor = localSystem.actorOf(Props[RemoteCalc],"localActor")

  scala.io.StdIn.readLine()
  localSystem.terminate()
}

// Identify消息确认机制是一种Actor沟通模式，所以我们需要
// 构建一个RemoteCalc Actor，把程序包嵌在这个Actor里面。
// 当receive收到确认消息ActorIdentity后获取ActorRef运算程序。
class RemoteCalc extends Actor with ActorLogging {
  val path = "akka.tcp://remoteSystem@127.0.0.1:2552/user/supervisorActor/calculator"

  context.actorSelection(path) ! Identify(path)  //send req for ActorRef

  import context.dispatcher
  implicit val timeout = Timeout(5 seconds)

  override def receive: Receive = {
    case ActorIdentity(p,someRef) if p.equals(path) =>
      someRef foreach { calcActor =>

        calcActor ! Clear
        calcActor ! Num(13.0)
        calcActor ! Mul(1.5)
        ((calcActor ? GetResult).mapTo[String]) foreach println

        calcActor ! Div(0.0)
        calcActor ! Div(1.5)
        calcActor ! Add(100.0)
        ((calcActor ? GetResult).mapTo[String]) foreach println

      }
  }
}