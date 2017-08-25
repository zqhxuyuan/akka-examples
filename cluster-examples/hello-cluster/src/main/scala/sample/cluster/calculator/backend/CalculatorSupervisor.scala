package sample.cluster.calculator.backend

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import sample.cluster.calculator.Messages.RegisterBackendActor

import scala.concurrent.duration._

/**
  * Created by zhengqh on 17/8/15.
  */
// mathOps equals role
class CalculatorSupervisor(mathOps: String) extends Actor {
  def decider: PartialFunction[Throwable,SupervisorStrategy.Directive] = {
    case _: ArithmeticException => SupervisorStrategy.Resume
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds){
      decider.orElse(SupervisorStrategy.defaultDecider)
    }

  val calcActor = context.actorOf(CalcFunctions.propsFuncs,"calcFunction")
  val cluster = Cluster(context.system)

  // subscribe member up event to akka-cluster before member startup
  override def preStart(): Unit = {
    cluster.subscribe(self,classOf[MemberUp])
    super.preStart()
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
    super.postStop()
  }

  override def receive: Receive = {
    case MemberUp(m) =>
      if (m.hasRole("frontend")) {
        val frontendActor = context.actorSelection(RootActorPath(m.address)+"/user/frontend")
        frontendActor ! RegisterBackendActor(mathOps)
      }
    case msg@ _ => calcActor.forward(msg)
  }

}
