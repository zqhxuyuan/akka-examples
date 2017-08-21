package concurrency.test

import akka.actor._
import akka.testkit._
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration._

class AnnoyingSpec extends TestKit(ActorSystem("AltimeterSpec"))
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll {

  "The AnnoyingActor" should {
    "say Hello!!!" in {
      val a = system.actorOf(Props(new AnnoyingActor(testActor)))
      expectMsg("Hello!!!")
      system.stop(a)
    }
  }

  "The NiceActor" should {
    "say Hi" in {
      val a = system.actorOf(Props(new NiceActor(testActor)))
      expectMsg("Hi")
      system.stop(a)
    }
  }

  "The AnnoyingActor Probe" should {
    "say Hello!!!" in {
      val p = TestProbe()
      val a = system.actorOf(Props(new AnnoyingActor(p.ref))) // We're expecting the message on the unique TestProbe, // not the general testActor that the  ̧T provides p.expectMsg("Hello!!!")
      system.stop(a)
    }
  }

}