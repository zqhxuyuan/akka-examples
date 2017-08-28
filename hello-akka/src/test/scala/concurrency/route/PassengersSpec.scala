package concurrency.route

import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import concurrency.route.Passenger.FastenSeatbelts
import org.scalatest._
import org.scalatest.matchers._

import scala.concurrent.duration._

trait TestDrinkRequestProbability
  extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin = 0.millis
  override val requestUpper = 2.millis
}

class PassengersSpec extends TestKit(ActorSystem("TestPassenger", ConfigFactory.parseString(
  """
    |zzz.akka.avionics.drinks = [
    |  "Apple","Orange"
    |]
  """.stripMargin)))
  with ImplicitSender
  with WordSpecLike
  with MustMatchers {

  import akka.event.Logging.Info
  import akka.testkit.TestProbe

  var seatNumber = 9
  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(testActor) with TestDrinkRequestProbability), s"Pat_Metheny-$seatNumber-B")
  }

  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString must include ("fastening seatbelt")
      }
    }
  }
}
