package concurrency.supervisor

import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}
import akka.actor.{ActorKilledException, ActorInitializationException, Actor}
import scala.concurrent.duration._

object IsolatedLifeCycleSupervisor {
  // Messages in case we want people to be able to wait for us to finish starting:
  case object WaitForStart
  case object Started
}

trait IsolatedLifeCycleSupervisor extends Actor {
  import IsolatedLifeCycleSupervisor._

  def receive = {
    // Emit Started msg to those who are eagerly awaiting:
    case WaitForStart =>
      sender ! Started

    // We don't handle anything else, but we give an error message stating the error:
    case m =>
      throw new Exception(s"Don't call ${self.path.name} directly ($m).")
  }

  // Subclass has to implement:
  def childStarter(): Unit

  // Only start children once we have started:
  final override def preStart() { childStarter() }

  // Don't call preStart(), which would otherwise be the default behaviour:
  final override def postRestart(reason: Throwable) {}

  // Don't stop the children, which would otherwise be the default behaviour:
  final override def preRestart(reason: Throwable, message: Option[Any]) {}
}

abstract class IsolatedResumeSupervisor(
                                         maxNrRetries: Int = -1,
                                         withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException         => Stop
    case _: Exception                    => Resume // 恢复
    case _                               => Escalate
  }
}

abstract class IsolatedStopSupervisor(
                                       maxNrRetries: Int = -1,
                                       withinTimeRange: Duration = Duration.Inf)
  extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>

  override val supervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException         => Stop
    case _: Exception                    => Stop // 停止
    case _                               => Escalate
  }
}