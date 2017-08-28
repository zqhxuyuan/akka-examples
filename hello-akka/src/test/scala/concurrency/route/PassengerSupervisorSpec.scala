package concurrency.route

import akka.actor.{ActorSystem, Actor, ActorRef, Props}
import akka.testkit.{TestKit, ImplicitSender}
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import org.scalatest.{WordSpecLike, MustMatchers, BeforeAndAfterAll}
import org.scalatest.matchers._

// A specialized configuration we'll inject into the
// ActorSystem so we have a known quantity we can test with
object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString(
    """
      |zzz.akka.avionics.passengers = [
      |  [ "Kelly Franqui", "20", "C"],
      |  [ "Kenya Jolicoeur", "24", "A" ],
      |  [ "Christian Piche", "25", "B" ],
      |  [ "Z qh", "25", "B" ],
      |  [ "X xx", "25", "B" ]
      |]
    """.stripMargin)
}

// We don't want to work with "real" passengers.  This mock
// passenger will be much easier to verify things with
trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor =
    new Actor {
      def receive = {
        case m => callButton ! m
      }
    }
}

// The Test class injects the configuration into the ActorSystem
class PassengerSupervisorSpec
  extends TestKit(ActorSystem("PassengerSupervisorSpec", PassengerSupervisorSpec.config))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with MustMatchers {

  import PassengerSupervisor._

  // Clean up the system when all the tests are done
  override def afterAll() {
    system.terminate()
  }

  "PassengerSupervisor" should {
    "work" in {
      // Get our SUT
      val a = system.actorOf(Props(new PassengerSupervisor(testActor) with TestPassengerProvider))

      // Grab the BroadcastRouter
      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgType[PassengerBroadcaster].broadcaster
      broadcaster ! "Hithere"

      // All 5 passengers should say "Hithere"
      expectMsg("Hithere")
      expectMsg("Hithere")
      expectMsg("Hithere")
      expectMsg("Hithere")
      expectMsg("Hithere")
      // And then nothing else!
      expectNoMsg(100.milliseconds)
      // Ensure that the cache works
      a ! GetPassengerBroadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))
    }
  }
}
