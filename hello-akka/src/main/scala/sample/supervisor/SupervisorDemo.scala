package sample.supervisor

import akka.actor._
import scala.util.Random
import scala.concurrent.duration._

object ChildActor {
  class RndException(msg: String) extends Exception(msg)
  def props = Props[ChildActor]
}

class ChildActor extends Actor with ActorLogging {
  import ChildActor._

  override def receive: Receive = {
    case msg: String => {   //任意产生一些RndExcption
      if (Random.nextBoolean())
        throw new RndException("Any Exception!")
      else
        log.info(s"Processed message: $msg !!!")
    }
  }

  override def preStart(): Unit = {
    log.info("ChildActor Started.")
    super.preStart()
  }

  //在重启时preRestart是在原来的Actor实例上调用preRestart的
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"Restarting ChildActor for ${reason.getMessage}...")
    message match {
      case Some(msg) =>
        log.info(s"Exception message: ${msg.toString}")
        self ! msg       //把异常消息再摆放到信箱最后
      case None =>
    }
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    log.info(s"Restarted ChildActor for ${reason.getMessage}...")
  }

  override def postStop(): Unit = {
    log.info(s"Stopped ChildActor.")
    super.postStop()
  }

}

//监管父级
class Parent extends Actor with ActorLogging {

  def decider: PartialFunction[Throwable,SupervisorStrategy.Directive] = {
    case _: ChildActor.RndException => SupervisorStrategy.Restart
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 30, withinTimeRange = 3 seconds) {
      decider.orElse(SupervisorStrategy.defaultDecider)
    }

  val childActor = context.actorOf(ChildActor.props,"childActor")
  override def receive: Receive = {
    case msg@ _ => childActor ! msg    //把所有收到的消息都转给childActor
  }

}

object SupervisorDemo extends App{

  val system = ActorSystem("testSystem")
  val parentActor = system.actorOf(Props[Parent],"parentActor")

  parentActor ! "Hello 1"
  parentActor ! "Hello 2"
  parentActor ! "Hello 3"
  parentActor ! "Hello 4"
  parentActor ! "Hello 5"

  Thread.sleep(5000)
  system.terminate()

}
