package sample.cluster.singleton

import akka.actor._
import akka.cluster._
import akka.cluster.singleton._
import akka.pattern._
import akka.persistence._
import akka.persistence.journal.leveldb._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object SingletonActor {
  sealed trait Command
  case object Dig extends Command
  case object Plant extends Command
  case object AckDig extends Command    //acknowledge
  case object AckPlant extends Command   //acknowledge

  case object Disconnect extends Command   //force node to leave cluster
  case object CleanUp extends Command      //clean up when actor ends

  sealed trait Event
  case object AddHole extends Event
  case object AddTree extends Event

  // Actor内部的状态
  case class State(nHoles: Int, nTrees: Int, nMatches: Int)

  def create(port: Int) = {
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port")
      .withFallback(ConfigFactory.parseString("akka.cluster.roles=[singleton]"))
      .withFallback(ConfigFactory.load("singleton"))
    val singletonSystem = ActorSystem("SingletonClusterSystem",config)

    startupSharedJournal(singletonSystem, (port == 2551), path =
      ActorPath.fromString("akka.tcp://SingletonClusterSystem@127.0.0.1:2551/user/store"))

    // 集群的单例管理类会创建唯一的SingletonActor
    val singletonManager = singletonSystem.actorOf(ClusterSingletonManager.props(
      singletonProps = Props[SingletonActor],
      terminationMessage = CleanUp,
      settings = ClusterSingletonManagerSettings(singletonSystem).withRole(Some("singleton"))
    ), name = "singletonManager")

  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) =>
        SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }
  }
}

class SingletonActor extends PersistentActor with ActorLogging {
  import SingletonActor._
  val cluster = Cluster(context.system)

  var freeHoles = 0
  var freeTrees = 0
  var ttlMatches = 0

  override def persistenceId = self.path.parent.name + "-" + self.path.name

  def updateState(evt: Event): Unit = evt match {
    case AddHole =>
      if (freeTrees > 0) {
        ttlMatches += 1
        freeTrees -= 1
      } else freeHoles += 1
    case AddTree =>
      if (freeHoles > 0) {
        ttlMatches += 1
        freeHoles -= 1
      } else freeTrees += 1

  }

  override def receiveRecover: Receive = {
    case evt: Event => updateState(evt)
    case SnapshotOffer(_,ss: State) =>
      freeHoles = ss.nHoles
      freeTrees = ss.nTrees
      ttlMatches = ss.nMatches
  }

  override def receiveCommand: Receive = {
    case Dig =>
      persist(AddHole){evt =>
        updateState(evt)
      }
      sender() ! AckDig   //notify sender message received
      log.info(s"State on ${cluster.selfAddress}:freeHoles=$freeHoles,freeTrees=$freeTrees,ttlMatches=$ttlMatches")

    case Plant =>
      persist(AddTree) {evt =>
        updateState(evt)
      }
      sender() ! AckPlant   //notify sender message received
      log.info(s"State on ${cluster.selfAddress}:freeHoles=$freeHoles,freeTrees=$freeTrees,ttlMatches=$ttlMatches")

    case Disconnect =>  //this node exits cluster. expect switch to another node
      log.info(s"${cluster.selfAddress} is leaving cluster ...")
      cluster.leave(cluster.selfAddress)
    case CleanUp =>
      //clean up ...
      self ! PoisonPill
  }

}