package concurrency.pilots

import akka.actor.{Actor, ActorRef, Terminated}
import concurrency.pilots.Plane.{Controls, GiveMeControl}

import scala.concurrent.{Await, Future}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait PilotProvider {
  def pilot: Actor = new Pilot
  def copilot: Actor = new Copilot
}

object Pilots {
  case object ReadyToGo
  case object RelinquishControl
}

class Pilot
  extends Actor {
  import Pilots._
  import Plane._
  // timeout needed for using ActorContext.actorSelection:
  implicit val timeout = Timeout(2.seconds)

  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      // Pilot作为Plane的子Actor, 在Pilots中可以通过context.parent获取Plane的引用
      // 即Pilot Actor向Plane Actor发送GiveMeControl的引用
      // 在Plane中,会创建控制台, 当Pilot想要获取控制权时, Plane会返回ControlSurfaces的引用给Pilot
      context.parent ! GiveMeControl

      var copilotFuture: Future[ActorRef] =
        for (cop <- context.actorSelection("../" + copilotName).resolveOne()) yield cop
      copilot = Await.result(copilotFuture, 1.second)

      // TODO 主驾驶员监控副驾驶员
      context.watch(copilot)
    case Controls(controlSurfaces) =>
      // Pilot收到Plane返回给他的控制权Actor
      controls = controlSurfaces
      println("主驾驶员获取控制台")
    case Terminated(_) =>
      self ! ReadyToGo
      println("副驾驶员挂掉")
  }
}

class Copilot extends Actor {
  import Pilots._
  // timeout needed for using ActorContext.ActorSelection:
  implicit val timeout = Timeout(2.seconds)

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      var pilotFuture: Future[ActorRef] =
        for (pil <- context.actorSelection("../" + pilotName).resolveOne()) yield pil
      pilot = Await.result(pilotFuture, 1.second)
      // 副驾驶员监控主驾驶员
      context.watch(pilot)
    case Terminated(_) => // there is only one pilot and we only need to know s/he was terminated
      context.parent ! GiveMeControl
      println("主驾驶员挂掉")
    case Controls(controlSurfaces) =>
      controls = controlSurfaces
      println("副驾驶员获取控制台")
  }
}
