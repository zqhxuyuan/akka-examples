package concurrency.avionics

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import concurrency.avionics.Altimeter._

import scala.concurrent.Await
import scala.concurrent.duration._
// The futures created by the ask syntax need an
// execution context on which to run, and we will use the
// default global instance for that context
import scala.concurrent.ExecutionContext.Implicits.global

// 航空电子设备
object AvionicsDemo {
  implicit val timeout = Timeout(5.seconds) // needed for '?' below
  val system = ActorSystem("PlaneSimulation")

  val plane = system.actorOf(Props[Plane], "Plane")

  def main(args: Array[String]) {
    val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5.seconds)

    control ? ControlSurfaces.StickBack(1f)

    currentAltitude()

    control ? ControlSurfaces.StickBack(0f)

    currentAltitude()

    control ? ControlSurfaces.StickBack(0.5f)

    currentAltitude()

    control ? ControlSurfaces.StickBack(0f)

    currentAltitude()

    system.scheduler.scheduleOnce(10.seconds) {
      system.terminate()
    }
  }

  def currentAltitude(): Unit = {
    val AltitudeResult(current) = Await.result((plane ? GetAltitude).mapTo[AltitudeResult], 5.seconds)
    println("当前高度:"+current)
  }
}