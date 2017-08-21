package concurrency.supervisor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import concurrency.avionics.{ControlSurfaces, EventSource}
import concurrency.flight.LeadFlightAttendantProvider
import concurrency.supervisor.IsolatedLifeCycleSupervisor.WaitForStart

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Plane {

  // Returns control-surfaces of the Plane to the Actor that asks for them
  case object GiveMeControl

  // Ch7 workaround allowing Avionics main method to still request controls without Controls-wrapper:
  case object GiveMainControl

  // Response to GiveMeControl message:
  case class Controls(controls: ActorRef)

  def apply() = new Plane with AltimeterProvider with PilotProvider with LeadFlightAttendantProvider
}

// The Plane has the controls, so someone can get the controls by sending a GiveMeControl message to the Plane.
// The Plane also has the Altimeter, so we build an Altimeter also and its ActorRef goes into our control-surface:
class Plane extends Actor with ActorLogging {
  this: AltimeterProvider
    with PilotProvider
    with LeadFlightAttendantProvider =>

  import Altimeter._
  import Plane._

  import scala.concurrent.ExecutionContext.Implicits.global

  val cfgstr = "zzz.akka.avionics.flightcrew"
  val config = context.system.settings.config

  val pilotName = config.getString(s"$cfgstr.pilotName")
  val copilotName = config.getString(s"$cfgstr.copilotName")
  val attendantName = config.getString(s"$cfgstr.leadAttendantName")

  // Need an implicit Timeout when using ask ("?") method. And also for awaiting result of ActorSelection Future:
  implicit val askTimeout = Timeout(1.second)

  // We can wait around for a result, so long as all the calls to actorForControls and actorForPilots are being made
  // during application set up or other occasional message, this is ok. Currently these methods are called by:
  // preStart() when responding to messages: GiveMainControl, GiveMeControl
  def actorForControls(name: String) = {
    val equipmentActorFuture: Future[ActorRef] =
      for (eaf <- context.actorSelection("Equipment/" + name).resolveOne()) yield eaf
    val controlsActor: ActorRef = Await.result(equipmentActorFuture, 1.second)
    controlsActor
  }

  def actorForPilots(name: String) = {
    val pilotActorFuture: Future[ActorRef] =
      for (paf <- context.actorSelection("Pilots/" + name).resolveOne()) yield paf
    val pilotActor: ActorRef = Await.result(pilotActorFuture, 1.second)
    pilotActor
  }

  // new Altimeter and ControlSurfaces use self-defined supervisor strategy
  // /user/Plane/Equipment/            飞机上的设备, 包括高度计/控制台
  //                      |-- Altimeter
  //                      |-- ControlSurfaces
  def startEquipment(): Unit = {
    val controls = context.actorOf(
      Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          val alt = context.actorOf(Props(newAltimeter), "Altimeter")
          context.actorOf(Props(new ControlSurfaces(alt)), "ControlSurfaces")
        }
      }), "Equipment"
    )
    // blocking (Await) is ok for Plane start-up
    Await.result(controls ? WaitForStart, 1.second)
  }

  // 飞机上的人, 主飞行员,备份飞行员, 乘务员
  // /user/Plane/Pilots/
  //                   |-- Pilot
  //                   |-- Copilot
  //            /LeadFlightAttendant
  //                   |-- FlightAttendant1
  //                   |-- FlightAttendant2
  def startPeople(): Unit = {
    val plane = self
    // We depend on the Actor structure beneath the Plane by using actorSelection().
    // Hopefully this is change-resilient, since we'll be the ones making the changes...
    val controls = actorForControls("ControlSurfaces")
    val altimeter = actorForControls("Altimeter")

    val people = context.actorOf(
      Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
        override def childStarter(): Unit = {
          context.actorOf(Props(newPilot(plane, controls, altimeter)), pilotName)
          context.actorOf(Props(newCopilot(plane, altimeter)), copilotName)
        }
      }), "Pilots"
    )
    // Use default strategy for flight attendants i.e. restart indefinitely:
    context.actorOf(Props(newFlightAttendant), attendantName)
    // blocking ok for Plane start-up
    Await.result(people ? WaitForStart, 1.second)
  }

  override def preStart(): Unit = {
    import EventSource.RegisterListener
    import Pilots.ReadyToGo

    // Get the children started. Starting order matters:
    startEquipment()
    startPeople()

    // Bootstrap the rest of the ActorSystem:
    actorForControls("Altimeter") ! RegisterListener(self)
    actorForPilots(pilotName) ! ReadyToGo
    actorForPilots(copilotName) ! ReadyToGo
  }

  def receive = {
    case GiveMainControl =>
      log info "Plane giving control to Main..."
      sender ! actorForControls("ControlSurfaces")
    case GiveMeControl =>
      log info "Plane giving control..."
      sender ! Controls(actorForControls("ControlSurfaces"))
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }
}