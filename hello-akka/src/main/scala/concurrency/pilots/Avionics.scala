package concurrency.pilots

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import concurrency.avionics.ControlSurfaces

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

// 航空电子设备
object Avionics {
  implicit val timeout = Timeout(5.seconds) // needed for '?' below
  val system = ActorSystem("PlaneSimulation")

  // 造一台飞机
  val plane = system.actorOf(Props[Plane], "Plane")

  def main(args: Array[String]) {
    // Grab the controls 获取控制器
    // 由于?返回的是一个Future[T], 通过mapTo或者flatMap转换为一个ActorRef

    // Exception in thread "main" java.lang.ClassCastException:
    //   Cannot cast concurrency.pilots.Plane$Controls to akka.actor.ActorRef
    //val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5.seconds)

    val control = Await.result((plane ? Plane.GiveMainControl).mapTo[ActorRef], 5.seconds)

    // Takeoff!
    system.scheduler.scheduleOnce(200.millis) {
      control ! ControlSurfaces.StickBack(1f)
    }
    // Level out
    system.scheduler.scheduleOnce(1.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Climb
    system.scheduler.scheduleOnce(3.seconds) {
      control ! ControlSurfaces.StickBack(0.5f)
    }
    // Level out
    system.scheduler.scheduleOnce(4.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Shut down
    system.scheduler.scheduleOnce(5.seconds) {
      system.terminate()
    }
  }
}