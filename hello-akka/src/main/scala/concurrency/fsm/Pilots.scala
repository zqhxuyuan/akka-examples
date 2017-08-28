package concurrency.fsm

/**
  * Updated code to get round deprecated ActorContext.actorFor syntax.
  * See also this discussion:
  * http://stackoverflow.com/questions/22951549/how-do-you-replace-actorfor
  */
import akka.actor.{FSM, Actor, ActorRef, Terminated}
import akka.util.Timeout
import concurrency.pilots.Plane.GiveMeControl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

// 创建Pilot,CoPilot需要引用飞机的其他组件. 比如飞机,控制台,高度计等
trait PilotProvider {
  def newPilot(plane: ActorRef, controls: ActorRef, altimeter: ActorRef, heading: ActorRef): Actor =
    new Pilot(plane, controls, altimeter, heading) with DrinkingProvider with FlyingProvider
  def newCopilot(plane: ActorRef, altimeter: ActorRef): Actor =
    new Copilot(plane, altimeter)
}

// TODO 自动飞行员, 如果两个飞行员都挂了,开启自动模式
object Pilots {
  case object ReadyToGo
  case object RelinquishControl // 放弃控制权, 交给另一个飞行员
}

// Dependency inject stuff the Pilot will need straight into its constructor:
class Pilot(plane: ActorRef, var controls: ActorRef, altimeter: ActorRef, heading: ActorRef)
  extends Actor {
  this: DrinkingProvider with FlyingProvider =>

  import Pilots._
  import Plane._
  import Altimeter._
  import ControlSurfaces._
  import DrinkingBehaviour._
  import FlyingBehaviour._
  import FSM._

  // timeout needed for using ActorContext.actorSelection:
  implicit val timeout = Timeout(2.seconds)

  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.copilotName")

  def setCourse(flyer: ActorRef) {
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
  }

  override def preStart() {
    // Create our children
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading,altimeter), "FlyingBehaviour")
  }

  /*
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
  */

  def actorFor(actorPath: String) = {
    var future: Future[ActorRef] =
      for (cop <- context.actorSelection(actorPath).resolveOne()) yield cop
    val actor = Await.result(future, 1.second)
    actor
  }

  // We've pulled the bootstrapping code out into a separate
  // receive method.  We'll only ever be in this state once,
  // so there's no point in having it around for long
  def bootstrap: Receive = {
    case ReadyToGo =>
      val copilot = actorFor("../" + copilotName)
      val flyer = actorFor("FlyingBehaviour")
      flyer ! SubscribeTransitionCallBack(self)
      setCourse(flyer)
      context.become(sober(copilot, flyer))
  }

  // The 'sober' behaviour
  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => // We're already sober
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }
  // The 'tipsy' behaviour
  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => // We're already tipsy
    case FeelingLikeZaphod => becomeZaphod(copilot, flyer)
  }
  // The 'zaphod' behaviour
  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober => becomeSober(copilot, flyer)
    case FeelingTipsy => becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod => // We're already Zaphod
  }

  // The 'idle' state is merely the state where the Pilot does nothing at all
  def idle: Receive = {
    case _ =>
  }

  // Updates the FlyingBehaviour with sober calculations and
  // then becomes the sober behaviour
  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }
  // Updates the FlyingBehaviour with tipsy calculations and
  // then becomes the tipsy behaviour
  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }
  // Updates the FlyingBehaviour with zaphod calculations
  // and then becomes the zaphod behaviour
  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  // At any time, the FlyingBehaviour could go back to an
  // Idle state, which means that our behavioural changes don't matter any more
  override def unhandled(msg: Any): Unit = {
    msg match {
      case Transition(_, _, Flying) =>
        setCourse(sender)
      case Transition(_, _, Idle) =>
        context.become(idle)
      // Ignore these two messages from the FSM rather than have them go to the log
      case Transition(_, _, _) =>
      case CurrentState(_, _) =>
      case m => super.unhandled(m)
    }
  }

  // Initially we start in the bootstrap state
  def receive = bootstrap
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
