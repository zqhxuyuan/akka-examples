package concurrency.supervisor

/**
  * Updated code to get round deprecated ActorContext.actorFor syntax.
  * See also this discussion:
  * http://stackoverflow.com/questions/22951549/how-do-you-replace-actorfor
  */
import akka.actor.{Actor, ActorRef, Terminated}
import akka.util.Timeout
import concurrency.pilots.Plane.GiveMeControl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

// 创建Pilot,CoPilot需要引用飞机的其他组件. 比如飞机,控制台,高度计等
trait PilotProvider {
  def newPilot(plane: ActorRef, controls: ActorRef, altimeter: ActorRef): Actor = new Pilot(plane, controls, altimeter)
  def newCopilot(plane: ActorRef, altimeter: ActorRef): Actor = new Copilot(plane, altimeter)
}

// TODO 自动飞行员, 如果两个飞行员都挂了,开启自动模式
object Pilots {
  case object ReadyToGo
  case object RelinquishControl // 放弃控制权, 交给另一个飞行员
}

// Dependency inject stuff the Pilot will need straight into its constructor:
class Pilot(plane: ActorRef, var controls: ActorRef, altimeter: ActorRef)
  extends Actor {
  import Pilots._
  import Plane._
  // timeout needed for using ActorContext.actorSelection:
  implicit val timeout = Timeout(2.seconds)

  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      // find co-pilot
      var copilotFuture: Future[ActorRef] = for (cop <- context.actorSelection("../" + copilotName).resolveOne()) yield cop
      // wait until successfully create co-pilot, and do nothing then
      copilot = Await.result(copilotFuture, 1.second)
      // TODO 主驾驶员是否需要监控副驾驶员
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
  }
}

// 副驾驶员, 与驾驶员的区别是, 它没有控制台的引用, 因为只有驾驶员才有资格操作控制台
class Copilot(plane: ActorRef, altimeter: ActorRef) extends Actor {
  import Pilots._
  implicit val timeout = Timeout(2.seconds)

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      // find main pilot
      var pilotFuture: Future[ActorRef] = for (pil <- context.actorSelection("../" + pilotName).resolveOne()) yield pil
      pilot = Await.result(pilotFuture, 1.second)
      // 监控主驾驶员, 如果主驾驶员死了, 副驾驶员应该接手主驾驶员的工作, 要不然飞机就坠毁了
      context.watch(pilot)
    case Terminated(_) =>
      // there is only one pilot and we only need to know s/he was terminated
      // 当主驾驶员死了时,他会发送一条终止的消息给副驾驶员,因为副驾驶员监控了主驾驶员
      // 当副驾驶员收到主驾驶员死了的消息后,他会向飞机申请控制权,取得控制台的操作权限
      plane ! GiveMeControl
  }
}
