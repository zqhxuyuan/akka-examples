package concurrency.pilots

import akka.actor._
import akka.testkit.{TestActorRef, TestKit, ImplicitSender}
import akka.util.Timeout
import concurrency.flight.LeadFlightAttendantProvider
import concurrency.pilots.Plane.GiveMeControl
import org.scalatest._
import org.scalatest.matchers._

import scala.concurrent.Await

class TestPilotsSpec extends TestKit(ActorSystem("TestPilotsSpec"))
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll {

  override def afterAll() { system.terminate() }

  "Pilots" should {
    "give me control" in {
      val a = TestActorRef(Props(Plane()))
      a ! GiveMeControl
    }

    "create pilots" in {
      val plane = TestActorRef[Plane].underlyingActor
      val pilot = plane.pilot
      val copilot = plane.copilot
      println("Pilot:"+pilot)
      println("CoPilot:"+copilot)
    }

    "pilots ha" in {
      val plane = TestActorRef[Plane].underlyingActor
      val pilot = plane.pilot
      val copilot = plane.copilot

      system.stop(pilot)
    }
  }
}

class FakePilot extends Actor {
  override def receive = {
    case _ =>
  }
}

object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
      zzz.akka.avionics.flightcrew.copilotName = "$copilotName"
      zzz.akka.avionics.flightcrew.pilotName = "$pilotName"
    """
}

