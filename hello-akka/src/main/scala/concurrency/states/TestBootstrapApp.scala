package concurrency.states

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask

/**
  * Created by zhengqh on 17/8/27.
  */
object TestBootstrapApp {
  def main(args: Array[String]) {
    val actorSystem = ActorSystem("Test")

    val actor1 = actorSystem.actorOf(Props[MyActor1])
    val actor2 = actorSystem.actorOf(Props(new MyActor2(actor1)))

    implicit val timeout = Timeout(5.seconds) // needed for '?' below



    actorSystem.terminate()
  }
}
class MyActor1 extends Actor {
  import MyActor2._

  def receive = {
    case NewDataArrived(metadata, data) =>
      println("receive " + data)
    case _ =>
  }
}

class MyActor2(destination: ActorRef) extends Actor {
  import MyActor2._

  // TODO how to inject bytes variable?
  var bytes: Vector[Byte] = null

  def needsBootstrapping: Receive = {
    case Bootstrap(_) =>
      context.become(bootstrapped)
  }

  def bootstrapped: Receive = {
    case PrepareForTransfer(metadata) =>
      context.become(transferReady(metadata))
  }

  def transferReady(metadata: Map[String, String]): Receive = {
    case Bytes(bytes) =>
      context.become(transferInProgress(metadata, Vector(bytes)))
  }

  def transferInProgress(metadata: Map[String, String],
                         data: Vector[Vector[Byte]]): Receive = {
    case Bytes(bytes) =>
      context.become(transferInProgress(metadata, data :+ bytes))
    case EOF =>
      destination ! NewDataArrived(metadata, data)
      context.become(bootstrapped)
  }

  def receive = needsBootstrapping
}

object MyActor2 {
  case class Bootstrap(msg: String)
  case class Bytes(bytes: Vector[Byte])
  case class PrepareForTransfer(metadata: Map[String, String])
  case object EOF
  case class NewDataArrived(metadata: Map[String, String], data: Vector[Vector[Byte]])
}
