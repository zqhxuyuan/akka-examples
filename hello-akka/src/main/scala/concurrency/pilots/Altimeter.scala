package concurrency.pilots

// Imports to help us create Actors, plus logging
import akka.actor.{Actor, ActorLogging}
import concurrency.test.{EventSource, ProductionEventSource}

// The duration package object extends Ints with some timing functionality
import scala.concurrent.duration._

import akka.actor.{Props, Actor, ActorSystem, ActorLogging}
import scala.concurrent.duration._

trait AltimeterProvider {
  def newAltimeter: Actor = Altimeter()
}

object Altimeter {

  // Received by Altimeter to inform about rate-of-climb changes:
  case class RateChange(amount: Float)

  // Sent by Altimeter to inform about current altitude:
  case class AltitudeUpdate(altitude: Double)

  // the apply-method constructs the Altimeter-Actor with an appropriate EventSource 'trait-implementation':
  def apply() = new Altimeter with ProductionEventSource
}

class Altimeter extends Actor with ActorLogging {
  this: EventSource =>

  import Altimeter._

  // We need an "Execution Context" for the Scheduler. This Actor's Dispatcher can serve that purpose.
  // The Scheduler's work will be dispatched on this Actor's own Dispatcher.
  implicit val ec = context.dispatcher

  // Maximum altitude of the plane in metres:
  val ceiling = 12500

  // Maximum rate-of-climb of the plane in metres per minute:
  val maxRateOfClimb = 1500

  // Varying rate-of-climb depnding on stick-movement:
  var rateOfClimb = 0f

  // Current altitude:
  var altitude = 0d

  // Change in altitude depends on how much time has passed, so need to measure time:
  var lastTick = System.currentTimeMillis()

  // Scheduled message-sending to our-self tells us when to update the altitude:
  val ticker = context.system.scheduler.schedule(100.millis, 100.millis, self, Tick)

  // The Tick-message we send to our-self re. updating the altitude:
  case object Tick

  def altimeterReceive: Receive = {
    // Rate of climb has changed ("reactive tense"):
    case RateChange(amount) =>
      // Truncate range to [-1, 1] before multiplying:
      rateOfClimb = amount.min(1.0f).max(-1.0f) * maxRateOfClimb //i.e. in range [-1500, 1500]
      log info s"Altimeter set rate-of-climb to $rateOfClimb."

    // Calculate new altitude:
    case Tick =>
      val tick = System.currentTimeMillis()
      altitude = altitude + ((tick - lastTick) / 60000.0) * rateOfClimb
      lastTick = tick

      sendEvent(AltitudeUpdate(altitude))
  }

  def receive = eventSourceReceive.orElse(altimeterReceive)

  // Cancel ticker when flight over:
  override def postStop(): Unit = ticker.cancel()
}


