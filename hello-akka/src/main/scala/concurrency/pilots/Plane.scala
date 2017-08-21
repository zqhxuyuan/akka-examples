package concurrency.pilots

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import concurrency.avionics.{ControlSurfaces, EventSource}
import concurrency.flight.{LeadFlightAttendant, LeadFlightAttendantProvider}

object Plane {

  // Returns control-surfaces of the Plane to the Actor that asks for them
  case object GiveMeControl

  // Ch7 workaround allowing Avionics main method to still request controls without Controls-wrapper:
  case object GiveMainControl

  // Response to GiveMeControl message:
  case class Controls(controls: ActorRef)

  def apply() = new Plane with AltimeterProvider with LeadFlightAttendantProvider
}

// The Plane has the controls, so someone can get the controls by sending a GiveMeControl message to the Plane. 
// The Plane also has the Altimeter, so we build an Altimeter also and its ActorRef goes into our control-surface:
class Plane extends Actor with ActorLogging {
  this: AltimeterProvider
    with LeadFlightAttendantProvider =>

  import Altimeter._
  import Plane._
  
  // Use Altimeter-companion-object's apply method to create the Actor:  
  val altimeter = context.actorOf(Props(Altimeter()), "Altimeter") // Altimeter's ActorRef now a child of Plane
  val controls = context.actorOf(Props(new ControlSurfaces(altimeter)), "ControlSurfaces")

  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config
  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val attendantName = config.getString(s"$cfgstr.leadAttendantName")

  val pilot = context.actorOf(Props[Pilot], pilotName)     
  val copilot = context.actorOf(Props[Copilot], copilotName)
  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()), attendantName)

  override def preStart(): Unit = {
    import EventSource.RegisterListener
    import Pilots._

    altimeter ! RegisterListener(self)
    List(pilot, copilot) foreach { _ ! Pilots.ReadyToGo }
  }

  def receive = {
    case GiveMainControl =>
      log info "Plane giving control to Main..."
      sender ! controls
    case GiveMeControl =>
      log info "Plane giving control..."
      sender ! Controls(controls)
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }
}