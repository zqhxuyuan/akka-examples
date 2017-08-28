package concurrency.fsm

// Imports to help us create Actors, plus logging
import akka.actor.{Actor, ActorLogging}
// The duration package object extends Ints with some timing functionality
import scala.concurrent.duration._

// 高度计
object Altimeter {
  // Sent to the Altimeter to inform it about rate-of-climb changes
  case class RateChange(amount: Float)

  // Sent by the Altimeter at regular intervals
  case class AltitudeUpdate(altitude: Double)
  case object GetAltitude
  case class AltitudeResult(altitude: Double)

  // change factory method
  def apply() = new Altimeter with ProductionEventSource
}

trait AltimeterProvider {
  def newAltimeter: Actor = Altimeter()
}

class Altimeter extends Actor with ActorLogging {
  this: EventSource =>

  import Altimeter._
  // We need an "ExecutionContext" for the scheduler.  This
  // Actor's dispatcher can serve that purpose.  The
  // scheduler's work will be dispatched on this Actor's own dispatcher
  implicit val ec = context.dispatcher

  // The maximum ceiling of our plane in 'feet' 上限
  val ceiling = 43000
  // The maximum rate of climb for our plane in 'feet per minute' 上升速度
  val maxRateOfClimb = 5000
  // The varying rate of climb depending on the movement of the stick 比率
  var rateOfClimb = 0f
  // Our current altitude 当前高度
  var altitude = 0d
  // As time passes, we need to change the altitude based on the time passed.
  // The lastTick allows us to figure out how much time has passed
  var lastTick = System.currentTimeMillis

  // We need to periodically update our altitude.  This
  // scheduled message send will tell us when to do that
  // 每隔100ms向自己发送一条Tick消息
  val ticker = context.system.scheduler.schedule(
    100.millis, 100.millis, self, Tick)

  // An internal message we send to ourselves to tell us to update our altitude
  case object Tick

  // partial function
  def altimeterReceive: Receive = {    // Our rate of climb has changed
    case RateChange(amount) =>
      // Truncate the range of 'amount' to [-1, 1] before multiplying
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb
      log.info(s"Altimeter changed rate of climb to $rateOfClimb.")
    // Calculate a new altitude 计算最新的高度
    case Tick =>
      val tick = System.currentTimeMillis
      altitude = altitude + ((tick - lastTick) / 60000.0) * rateOfClimb
      lastTick = tick

      // 发送高度更新的事件给每个监听器. 比如飞机作为高度计的一个监听器, AltitudeUpdate会被发送给飞机
      sendEvent(AltitudeUpdate(altitude))

    case GetAltitude =>
      log info(s"Altitude now is : $altitude")
      sender ! AltitudeResult(altitude)
  }

  // 必须定义一个receive方法, 使用partial function, 将多个逻辑串联在一起
  def receive = eventSourceReceive orElse altimeterReceive

  // Kill our ticker when we stop
  override def postStop(): Unit = ticker.cancel
}