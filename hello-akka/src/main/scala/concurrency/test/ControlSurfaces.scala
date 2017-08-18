package concurrency.test

import akka.actor.{Actor, ActorRef}

// The ControlSurfaces object carries messages for controlling the plane
object ControlSurfaces {
  // amount is a value between -1 and 1.  The altimeter
  // ensures that any value outside that range is truncated to be within it.
  case class StickBack(amount: Float) // 上升
  case class StickForward(amount: Float) // 下降
}

// Pass in the Altimeter as an ActorRef so that we can send messages to it
// 控制台持有高度计的引用, 这样我们(飞行员)可以将指令通过控制台发送RateChange消息给高度计
// 比如让飞机上升或下降, (飞行员)发送StickBack或StickForward指令给控制台.
class ControlSurfaces(altimeter: ActorRef) extends Actor {
  import Altimeter._
  import ControlSurfaces._

  def receive = {
    // Pilot pulled the stick back by a certain amount,
    // and we inform the Altimeter that we're climbing
    case StickBack(amount) =>
      altimeter ! RateChange(amount)
    // Pilot pushes the stick forward and we inform the
    // Altimeter that we're descending
    case StickForward(amount) =>
      altimeter ! RateChange(-1 * amount)

//    case GetAltitude =>
//      altimeter forward GetAltitude
//
//    case AltitudeResult(current) =>
//      sender ! current
  }
}