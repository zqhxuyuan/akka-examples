package concurrency.route

import akka.actor.{Actor, ActorRef, Props}

// The Lead is going to construct its own subordinates. We'll have a policy to vary that
trait AttendantCreationPolicy {
  // Feel free to make this configurable!
  val numberOfAttendants: Int = 8
  // create flight attendant
  def createAttendant: Actor = FlightAttendant()
}

// We'll also provide a mechanism for altering how we create the LeadFlightAttendant
trait LeadFlightAttendantProvider {
  def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {
  case object GetFlightAttendant
  case class Attendant(a: ActorRef)

  def apply() = new LeadFlightAttendant
    with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor {
  // self type with AttendantCreationPolicy trait
  this: AttendantCreationPolicy =>

  import LeadFlightAttendant._

  // After we've successfully spooled up the LeadFlightAttendant,
  // we're going to have it create all of its subordinates
  override def preStart() {
    import scala.collection.JavaConverters._

    val attendantNames = context.system.settings.config.getStringList("zzz.akka.avionics.flightcrew.attendantNames").asScala

    attendantNames take numberOfAttendants foreach { i =>
      // We create the actors within our context such that they are children of this Actor
      context.actorOf(Props(createAttendant), i)
    }
  }

  // 'children' is an Iterable. This method returns a random one
  def randomAttendant(): ActorRef = {
    context.children.take(
      scala.util.Random.nextInt(numberOfAttendants) + 1).last
  }

  def receive = {
    case GetFlightAttendant =>
      sender ! Attendant(randomAttendant())
    case m =>
      randomAttendant() forward m
  }
}

object FlightAttendantPathChecker {
  def main(args: Array[String]) {
    val system = akka.actor.ActorSystem("PlaneSimulation")

    val lead = system.actorOf(Props(
      new LeadFlightAttendant with AttendantCreationPolicy),
      "LeadFlightAttendant")

    import LeadFlightAttendant._

    lead ! GetFlightAttendant

    Thread.sleep(2000)
    system.terminate()
  }
}