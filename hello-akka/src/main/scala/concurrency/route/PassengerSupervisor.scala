package concurrency.route

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.routing.{Router, RoundRobinRoutingLogic, ActorRefRoutee, BroadcastPool}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by zhengqh on 17/8/28.
  */
object PassengerSupervisor {

  // Allows someone to request the BroadcastRouter
  case object GetPassengerBroadcaster

  // Returns the BroadcastRouter to the requestor
  case class PassengerBroadcaster(broadcaster: ActorRef)

  // Factory method for easy construction
  def apply(callButton: ActorRef) =
    new PassengerSupervisor(callButton) with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef) extends Actor {
  this: PassengerProvider =>

  import PassengerSupervisor._

  // We'll resume our immediate children instead of restarting them on an Exception
  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  // Internal messages we use to communicate between this Actor and its subordinate supervisor
  case class GetChildren(forSomeone: ActorRef)

  case class Children(children: Iterable[ActorRef], childrenFor: ActorRef)

  // We use preStart() to create our supervisor
  override def preStart() {
    // Note the supervisor is merely an anonymous actor
    context.actorOf(Props(new Actor {
      // shortcut
      val config = context.system.settings.config
      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => Escalate
        case _: ActorInitializationException => Escalate
        case _ => Stop
      }

      override def preStart() {
        import scala.collection.JavaConverters._
        import com.typesafe.config.ConfigList
        // Get our passenger names from the configuration
        val passengers = config.getList("zzz.akka.avionics.passengers")
        // Iterate through them to create the passenger children
        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList]
            .unwrapped().asScala.mkString("-")
            .replaceAllLiterally(" ", "_")
          // Convert spaces to underscores to comply with URI standard
          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }

      // Implement the receive method so that our parent can ask us for our created children
      def receive = {
        case GetChildren(forSomeone: ActorRef) =>
          sender ! Children(context.children, forSomeone)
      }
    }), "PassengersSupervisor")
  }

  def noRouter: Receive = {
    case GetPassengerBroadcaster =>
      val passengers = actorFor("PassengersSupervisor")
      passengers ! GetChildren(sender)
    case Children(passengers, destinedFor) =>
      // AKKA-2.2
      //val router = context.actorOf(Props().withRouter(BroadcastRouter(passengers.toSeq)), "Passengers")

      // AKKA-2.4+
      val router = context.actorOf(new BroadcastPool(5).props(Props(newPassenger(callButton))), "Passengers")

      // TODO how to pass passengers to router
      val routees = passengers.map(actor => ActorRefRoutee(actor)).toVector
      val paths = passengers.map(actor => actor.path.address)
      val brouter = Router(RoundRobinRoutingLogic(), routees)

      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }

  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster =>
      sender ! PassengerBroadcaster(router)
    case Children(_, destinedFor) =>
      destinedFor ! PassengerBroadcaster(router)
  }

  def receive = noRouter


  def actorFor(actorPath: String) = {
    implicit val timeout = Timeout(5.seconds) // needed for '?' below

    var future: Future[ActorRef] =
      for (cop <- context.actorSelection(actorPath).resolveOne()) yield cop
    val actor = Await.result(future, 1.second)
    actor
  }
}

