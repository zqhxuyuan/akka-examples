package concurrency.avionics

import akka.actor._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Plane {
  // Returns the control surface to the Actor that asks for them
  case object GiveMeControl
}

// We want the Plane to own the Altimeter and we're going to
// do that by passing in a specific factory we can use to build the Altimeter
class Plane extends Actor with ActorLogging {
  import Altimeter._
  import Plane._
  import EventSource._

  // 高度计
  val altimeter = context.actorOf(Props[Altimeter], "Altimeter")

  var currentAltitude = 0d

  // 控制台, 创建控制台时, 将上一步创建的高度计传给控制台
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")

  // 启动时, 将自己(飞机)注册为altimeter的一个监听器之一
  override def preStart() {
    altimeter ! RegisterListener(self)
  }

  def receive = {
    case GiveMeControl =>
      log info("Plane giving control.")
      // 将一个ActorRef对象返回给sender, 发送者(发送GiveMeControl的对象)就可以接收到一个ActorRef对象
      sender ! controls
    case AltitudeUpdate(altitude) =>
      currentAltitude = altitude
      log info(s"Altitude change to : $altitude")

    case GetAltitude =>
      altimeter forward GetAltitude

//    case AltitudeResult(current) =>
//      sender ! current
  }
}