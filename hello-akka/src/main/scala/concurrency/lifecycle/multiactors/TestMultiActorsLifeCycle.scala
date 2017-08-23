package concurrency.lifecycle.multiactors

import akka.actor.SupervisorStrategy._
import akka.actor.{OneForOneStrategy, _}

/**
[INFO] [08/22/2017 17:01:08.232] [main] [akka.remote.Remoting] Starting remoting
  [INFO] [08/22/2017 17:01:08.512] [main] [akka.remote.Remoting] Remoting started; listening on addresses :[akka.tcp://MyActors2@127.0.0.1:52712]
  [INFO] [08/22/2017 17:01:08.530] [main] [akka.cluster.Cluster(akka://MyActors2)] Cluster Node [akka.tcp://MyActors2@127.0.0.1:52712] - Starting up...
  [INFO] [08/22/2017 17:01:08.704] [main] [akka.cluster.Cluster(akka://MyActors2)] Cluster Node [akka.tcp://MyActors2@127.0.0.1:52712] - Registered cluster JMX MBean [akka:type=Cluster]
  [INFO] [08/22/2017 17:01:08.705] [main] [akka.cluster.Cluster(akka://MyActors2)] Cluster Node [akka.tcp://MyActors2@127.0.0.1:52712] - Started up successfully
  [WARN] [08/22/2017 17:01:08.770] [MyActors2-akka.actor.default-dispatcher-3] [akka.tcp://MyActors2@127.0.0.1:52712/system/cluster/core/daemon/downingProvider] Don't use auto-down feature of Akka Cluster in production. See 'Auto-downing (DO NOT USE)' section of Akka Cluster documentation.
  other actor pre start...
  OtherActor: Actor[akka://MyActors2/user/OtherActor#-1685680117]
  my actor pre start...
  MyActor: Actor[akka://MyActors2/user/MyActor#1525233454]
  my actor pre restart due to suicide first time
  my actor post stop...
  [ERROR] [08/22/2017 17:01:08.794] [MyActors2-akka.actor.default-dispatcher-2] [akka://MyActors2/user/MyActor] exception
  java.lang.Exception: exception
    at concurrency.actor.MyActor2$$anonfun$receive$1.applyOrElse(TestMultiActorsLifeCycle.scala:44)
    at akka.actor.Actor$class.aroundReceive(Actor.scala:513)
    at concurrency.actor.MyActor2.aroundReceive(TestMultiActorsLifeCycle.scala:10)
    at akka.actor.ActorCell.receiveMessage(ActorCell.scala:527)
    at akka.actor.ActorCell.invoke(ActorCell.scala:496)
    at akka.dispatch.Mailbox.processMailbox(Mailbox.scala:257)
    at akka.dispatch.Mailbox.run(Mailbox.scala:224)
    at akka.dispatch.Mailbox.exec(Mailbox.scala:234)
    at akka.dispatch.forkjoin.ForkJoinTask.doExec(ForkJoinTask.java:260)
    at akka.dispatch.forkjoin.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1339)
    at akka.dispatch.forkjoin.ForkJoinPool.runWorker(ForkJoinPool.java:1979)
    at akka.dispatch.forkjoin.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)

  my actor post restart
  my actor pre start...

  [WARN] [08/22/2017 17:01:09.001] [New I/O boss #3] [NettyTransport(akka://MyActors2)] Remote connection to [null] failed with java.net.ConnectException: Connection refused: /127.0.0.1:2551
  [WARN] [08/22/2017 17:01:09.004] [New I/O boss #3] [NettyTransport(akka://MyActors2)] Remote connection to [null] failed with java.net.ConnectException: Connection refused: /127.0.0.1:2552
  [WARN] [08/22/2017 17:01:09.044] [MyActors2-akka.remote.default-remote-dispatcher-5] [akka.tcp://MyActors2@127.0.0.1:52712/system/endpointManager/reliableEndpointWriter-akka.tcp%3A%2F%2FClusterSystem%40127.0.0.1%3A2552-1] Association with remote system [akka.tcp://ClusterSystem@127.0.0.1:2552] has failed, address is now gated for [5000] ms. Reason: [Association failed with [akka.tcp://ClusterSystem@127.0.0.1:2552]] Caused by: [Connection refused: /127.0.0.1:2552]
  [WARN] [08/22/2017 17:01:09.044] [MyActors2-akka.remote.default-remote-dispatcher-13] [akka.tcp://MyActors2@127.0.0.1:52712/system/endpointManager/reliableEndpointWriter-akka.tcp%3A%2F%2FClusterSystem%40127.0.0.1%3A2551-0] Association with remote system [akka.tcp://ClusterSystem@127.0.0.1:2551] has failed, address is now gated for [5000] ms. Reason: [Association failed with [akka.tcp://ClusterSystem@127.0.0.1:2551]] Caused by: [Connection refused: /127.0.0.1:2551]
  [INFO] [08/22/2017 17:01:09.052] [MyActors2-akka.actor.default-dispatcher-16] [akka://MyActors2/deadLetters] Message [akka.cluster.InternalClusterAction$InitJoin$] from Actor[akka://MyActors2/system/cluster/core/daemon/joinSeedNodeProcess-1#-638067725] to Actor[akka://MyActors2/deadLetters] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
  [INFO] [08/22/2017 17:01:09.053] [MyActors2-akka.actor.default-dispatcher-4] [akka://MyActors2/deadLetters] Message [akka.cluster.InternalClusterAction$InitJoin$] from Actor[akka://MyActors2/system/cluster/core/daemon/joinSeedNodeProcess-1#-638067725] to Actor[akka://MyActors2/deadLetters] was not delivered. [2] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
  other actor post stop...
  my actor post stop...
  [INFO] [08/22/2017 17:01:13.802] [MyActors2-akka.remote.default-remote-dispatcher-5] [akka.tcp://MyActors2@127.0.0.1:52712/system/remoting-terminator] Shutting down remote daemon.
  [INFO] [08/22/2017 17:01:13.805] [MyActors2-akka.remote.default-remote-dispatcher-5] [akka.tcp://MyActors2@127.0.0.1:52712/system/remoting-terminator] Remote daemon shut down; proceeding with flushing remote transports.
  [INFO] [08/22/2017 17:01:13.854] [MyActors2-akka.remote.default-remote-dispatcher-5] [akka.tcp://MyActors2@127.0.0.1:52712/system/remoting-terminator] Remoting shut down.
  [INFO] [08/22/2017 17:01:13.868] [MyActors2-akka.actor.default-dispatcher-4] [akka.cluster.Cluster(akka://MyActors2)] Cluster Node [akka.tcp://MyActors2@127.0.0.1:52712] - Shutting down...
  [INFO] [08/22/2017 17:01:13.870] [MyActors2-akka.actor.default-dispatcher-4] [akka.cluster.Cluster(akka://MyActors2)] Cluster Node [akka.tcp://MyActors2@127.0.0.1:52712] - Successfully shut down

  Process finished with exit code 0
  */
object TestMultiActorsLifeCycle {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActors2")

    // otherActor and myActor is not parent-child relationship, but in the same level
    // 先创建OtherActor,然后把OtherActor的引用传递给MyActor
    // MyActor在preStart中会监控OtherActor

    val otherActor = system.actorOf(Props[Actor1], "OtherActor")
    println("OtherActor: " + otherActor)

    val actor = system.actorOf(Props(new Actor2(otherActor)), "MyActor")
    println("MyActor: " + actor)

    // 3
    actor ! "suicide first time"

    Thread.sleep(5000)
    system.terminate()
  }
}

class Actor2(otherActor: ActorRef) extends Actor {
  override val supervisorStrategy = OneForOneStrategy() {
    case _ => Restart
  }

  // 2  7
  override def preStart() {
    println("my actor pre start...")
    context.watch(otherActor)
    super.preStart()
  }

  // 5  9
  override def postStop(): Unit = {
    println("my actor post stop...")
    super.postStop()
  }

  // 4
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("my actor pre restart due to " + throwable.getMessage + ", failed msg:" + message.get)
    super.preRestart(throwable, message)
  }

  // 6
  override def postRestart(throwable: Throwable) = {
    println("my actor post restart")
    super.postRestart(throwable)
  }

  def receive = {
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case _ =>
      throw new Exception("exception") // 3
  }
}

class Actor1 extends Actor {
  def receive = {
    case _ => //self ! Kill
  }

  // 1
  override def preStart(): Unit = {
    println("other actor pre start...")
    super.preStart()
  }

  // 8
  override def postStop() = {
    println("other actor post stop...")
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("other actor pre restart")
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println("other actor post restart")
    super.postRestart(throwable)
  }
}

