package sample.route.calculator.adaptive

import akka.actor._
import akka.cluster._
import com.typesafe.config.ConfigFactory
import Messages._

import scala.concurrent.duration._
import scala.util.Random

class RouterRunner extends Actor {
  val jobs = List(Add,Sub,Mul,Div)
  import context.dispatcher

  val calcRouter = context.actorOf(CalcRouter.props,"adaptive")
  context.system.scheduler.schedule(3.seconds, 3.seconds, self, "DoRandomMath")

  override def receive: Receive = {
    case  _ => calcRouter ! anyMathJob
  }
  def anyMathJob: MathOps =
    jobs(Random.nextInt(4))(Random.nextInt(100), Random.nextInt(100))
}

object AdaptiveRouterDemo extends App {

  BackEnd.create(2551)   //seed-node
  BackEnd.create(0)      //backend node

  Thread.sleep(2000)

  val config = ConfigFactory.parseString("akka.cluster.roles = [frontend]").
    withFallback(ConfigFactory.load("adaptive"))

  val calcSystem = ActorSystem("calcClusterSystem",config)
  val cluster = Cluster(calcSystem)

  cluster.registerOnMemberUp {
    val _ = calcSystem.actorOf(Props[RouterRunner],"frontend")
  }

  //val _ = calcSystem.actorOf(Props[RouterRunner],"frontend")

}
