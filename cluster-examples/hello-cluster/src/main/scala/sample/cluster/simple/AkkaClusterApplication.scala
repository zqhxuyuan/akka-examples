package sample.cluster.simple

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.typesafe.config.ConfigFactory

class AkkaCluster extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    //    case Start => log.info("Akka cluster working: " + Start)
    //    case msg => log.warning("Unknown nessage: " + msg)
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as Unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore
  }
}

object AkkaClusterApplication extends App {

  //  val system = ActorSystem("cluster-system", ConfigFactory.load().getConfig("MyCluster"))
  //  system.actorOf(Props[AkkaCluster], name = "clusterActor")

  Seq("2552", "2553").foreach { port =>
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load())
    val system = ActorSystem("cluster-system", config)
    system.actorOf(Props[AkkaCluster], name = "clusterActor")
  }
}