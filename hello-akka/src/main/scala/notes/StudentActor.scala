package notes

import akka.actor.{ActorLogging, ActorRef, Actor}
import akka.actor.Actor.Receive
import notes.TeacherProtocol._
import scala.concurrent.duration._

/**
 * Created by zqhxuyuan on 15-8-8.
 */
class StudentActor (teacherActorRef:ActorRef) extends Actor with ActorLogging {

  def receive = {
    case InitSignal=> {
      teacherActorRef ! QuoteRequest
    }

    case QuoteResponse(quoteString) => {
      log.info ("Received QuoteResponse from Teacher")
      log.info(s"Printing from Student Actor $quoteString")
    }
  }
}