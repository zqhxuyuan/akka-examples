package concurrency.lifecycle

import akka.actor._

class Actor1 extends Actor {
  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stop" =>
      self ! Kill
    case _ =>
      throw new Exception("Level2Actor exception")
  }

  override def preStart() {
    println(self + " preStart!!!")
    super.preStart()
  }
  override def postStop() = {
    println(self + " postStop!!!")
    super.postStop()
  }
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println(self + " preRestart!!!")
    super.preRestart(throwable, message)
  }
  override def postRestart(throwable: Throwable) = {
    println(self + " postRestart!!!")
    super.postRestart(throwable)
  }
}
