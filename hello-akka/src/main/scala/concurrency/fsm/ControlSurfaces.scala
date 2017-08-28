package concurrency.fsm

import akka.actor.{Actor, ActorRef}

// The ControlSurfaces object carries messages for controlling the plane
object ControlSurfaces {
  // amount is a value between -1 and 1.  The altimeter
  // ensures that any value outside that range is truncated to be within it.
  case class StickBack(amount: Float) // 上升
  case class StickForward(amount: Float) // 下降

  case class StickLeft(amount: Float) // 向左
  case class StickRight(amount: Float) // 向右
  case class HasControl(somePilot: ActorRef)
}

// Pass in the Altimeter as an ActorRef so that we can send messages to it
// 控制台持有高度计的引用, 这样我们(飞行员)可以将指令通过控制台发送RateChange消息给高度计
// 比如让飞机上升或下降, (飞行员)发送StickBack或StickForward指令给控制台.
class ControlSurfaces(plane: ActorRef,
                      altimeter: ActorRef,
                      heading: ActorRef
                     ) extends Actor {

  import ControlSurfaces._
  import Altimeter._
  import HeadingIndicator._

  // Instantiate the receive method by saying that the ControlSurfaces are controlled
  // by the dead letter office.  Effectively, this says that nothing's currently in control
  def receive = controlledBy(context.system.deadLetters)

  // As control is transferred between different entities, we will change the instantiated receive function
  // with new variants. This closure ensures that only the assigned pilot can control the plane
  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount) if sender == somePilot =>
      altimeter ! RateChange(amount)
    case StickForward(amount) if sender == somePilot =>
      altimeter ! RateChange(-1 * amount)

    case StickLeft(amount) if sender == somePilot =>
      heading ! BankChange(-1 * amount)
    case StickRight(amount) if sender == somePilot =>
      heading ! BankChange(amount)

    // Only the plane can tell us who's currently in control
    case HasControl(entity) if sender == plane =>
      // Become a new instance, where the entity, which the
      // plane told us about, is now the entity that controls the plane
      context.become(controlledBy(entity))
  }
}