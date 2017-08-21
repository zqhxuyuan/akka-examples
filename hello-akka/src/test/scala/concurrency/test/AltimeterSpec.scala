package concurrency.test

import akka.actor._
import akka.testkit._
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec"))
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll {

  import Altimeter._

  override def afterAll() { system.terminate() }

  // We'll instantiate a Helper class for every test, making things nicely reusable.
  class Helper {

    object EventSourceSpy {
      // The latch gives us fast feedback when something happens
      val latch = TestLatch(1)
    }

    // Our special derivation of EventSource gives us the hooks into concurrency
    trait EventSourceSpy extends EventSource {

      // when invoke this method, count down latch, then the latch turn to 0, then isOpen is true
      // so when would sendEvent be invoked? when update event occur!
      def sendEvent[T](event: T): Unit = EventSourceSpy.latch.countDown()

      // We don't care about processing the messages that
      // EventSource usually processes so we simply don't worry about them.
      def eventSourceReceive = Actor.emptyBehavior

    }

    // The slicedAltimeter constructs our Altimeter with the EventSourceSpy
    def slicedAltimeter = new Altimeter with EventSourceSpy

    // This is a helper method that will give us an ActorRef
    // and our plain ol' Altimeter that we can work with directly.
    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }
  }

  "Altimeter" should {

    "record rate of climb changes" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(1f))
      real.rateOfClimb must be (real.maxRateOfClimb)
    }

    "keep rate of climb changes within bounds" in new Helper {
      val (_, real) = actor()
      real.receive(RateChange(2f))
      real.rateOfClimb must be (real.maxRateOfClimb)
    }

    /** Fishing for results **/
    "calculate altitude changes" in new Helper {
      val ref = system.actorOf(Props(Altimeter()))
      // send msg to actor, but final result may not sequence
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)
      fishForMessage() {
        // if return false, wait true or until timeout
        case AltitudeUpdate(altitude) if altitude == 0f =>
          false
        case AltitudeUpdate(altitude) =>
          true
      }
    }

    /** Countdown to success **/
    "send events" in new Helper {
      val (ref, _) = actor()
      // we don't care about altitude, but sendEvent method call at last
      Await.ready(EventSourceSpy.latch, 1.second)
      // wait for the latch to become ready and then test to see if it’s open.
      EventSourceSpy.latch.isOpen must be (true)
    }
  }
}