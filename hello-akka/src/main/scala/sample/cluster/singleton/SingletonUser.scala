package sample.cluster.singleton

import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.cluster.singleton._
import scala.concurrent.duration._

object SingletonUser {
  def create = {
    val config = ConfigFactory.parseString("akka.cluster.roles=[frontend]")
      .withFallback(ConfigFactory.load("singleton"))
    val suSystem = ActorSystem("SingletonClusterSystem",config)

    val singletonProxy = suSystem.actorOf(ClusterSingletonProxy.props(
      singletonManagerPath = "/user/singletonManager",
      settings = ClusterSingletonProxySettings(suSystem).withRole(None)
    ), name= "singletonUser")


    import suSystem.dispatcher
    //send Dig messages every 2 seconds to SingletonActor through prox
    suSystem.scheduler.schedule(0.seconds,3.second,singletonProxy,SingletonActor.Dig)

    //send Plant messages every 3 seconds to SingletonActor through prox
    suSystem.scheduler.schedule(1.seconds,2.second,singletonProxy,SingletonActor.Plant)

    //send kill message to hosting node every 30 seconds
    suSystem.scheduler.schedule(10.seconds,15.seconds,singletonProxy,SingletonActor.Disconnect)
  }

}