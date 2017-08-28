package concurrency.fsm

import akka.actor.Actor
import scala.concurrent.duration._

// This trait allows us to create different Flight Attendants with different levels of responsiveness.
trait AttendantResponsiveness {
  // 最长的响应时间
  val maxResponseTimeMS: Int
  // 随机的响应间隔
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}

// 乘务员
object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)

  // By default we will make attendants that respond within 5 minutes
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }
}

class FlightAttendant extends Actor {
  this: AttendantResponsiveness =>

  import FlightAttendant._

  // bring the execution context into implicit scope for the scheduler below
  implicit val ec = context.dispatcher

  def receive = {
    case GetDrink(drinkname) => // 乘客要喝饮料了
      // We don't respond right away, but use the scheduler to ensure we do eventually
      context.system.scheduler.scheduleOnce(responseDuration, sender, Drink(drinkname))
  }
}