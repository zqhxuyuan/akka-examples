package concurrency.actor

import akka.actor._
import akka.testkit._
import org.scalatest._

class MyActorSpec extends TestKit(ActorSystem("MyActorSpec"))
  with WordSpecLike
  with MustMatchers
  with BeforeAndAfterAll
  with ParallelTestExecution {

  override def afterAll() {
    system.terminate()
  }

  def makeActor(): ActorRef = system.actorOf(Props[MyActor], "MyActor")

  "My Actor" should {
    "throw if constructed with the wrong name" in {
      //      evaluating {
      //        val a = system.actorOf(Props[MyActor])
      //      } must produce [Exception]
      an [Exception] should be thrownBy {
        val a = system.actorOf(Props[MyActor])
      }
    }
    "construct without exception" in {
      val a = makeActor()
      // The throw will cause the test to fail
    }
    "respond with a Pong to a Ping" in {
      val a = makeActor()
      a ! Ping
      expectMsg(Pong)
    }
  }


}