package concurrency.lifecycle.childsupervisorwithoutwatch

import akka.actor._

class Child1Actor(actorName: String = "child") extends Actor {
  def receive = {
    case _ => throw new Exception(s"$actorName exception")
  }

  override def preStart() {
    println(s"$actorName pre start..." + " @@" + self)
    super.preStart()
  }

  override def postStop(): Unit = {
    println(s"$actorName post stop..." + " @@" + self)
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println(s"$actorName pre restart due to " + throwable.getMessage +
      ", receive msg: " + message + " @@" + self)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println(s"$actorName post restart" + " @@" + self)
    super.postRestart(throwable)
  }
}
