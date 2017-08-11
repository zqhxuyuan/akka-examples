package sample.remote

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

import sample._

class RemoteActor extends Actor with ActorLogging {

  val SUCCESS = "SUCCESS"
  val FAILURE = "FAILURE"

  def receive = {
    case Start => {
      log.info("RECV event: " + Start)
    }
    case Stop => {
      log.info("RECV event: " + Stop)
    }
    case Shutdown(waitSecs) => {
      log.info("Wait to shutdown: waitSecs=" + waitSecs)
      Thread.sleep(waitSecs)
      log.info("Shutdown this system.")
      context.system.terminate()

    }
    case Heartbeat(id, magic) => log.info("RECV heartbeat: " + (id, magic))
    case Header(id, len, encrypted) => log.info("RECV header: " + (id, len, encrypted))
    case Packet(id, seq, content) => {
      val originalSender = sender
      log.info("RECV packet: " + (id, seq, content))
      originalSender ! (seq, SUCCESS)
    }
    case _ =>
  }
}


object AkkaServerBootableApplication {

  // Remote actor
  // http://agiledon.github.io/blog/2014/02/18/remote-actor-in-akka/
  val system = ActorSystem("remote-system", ConfigFactory.load().getConfig("MyRemoteServerSideActor"))
  val log = system.log
  log.info("Remote server side actor started: " + system)

  def startup = {
    system.actorOf(Props[RemoteActor], "remoteActor")
  }

  def shutdown = {
    system.terminate()
  }

}

object AkkaServerApplication extends App {

  val system = ActorSystem("remote-system", ConfigFactory.load().getConfig("MyRemoteServerSideActor"))
  val log = system.log
  log.info("Remote server actor started: " + system)

  system.actorOf(Props[RemoteActor], "remoteActor")

}