package concurrency.lifecycle.childwithwatch

import akka.actor.SupervisorStrategy._
import akka.actor._

import scala.concurrent.duration._
import scala.util.Random

/**
  * 对比Kill命令, 这里收到stopchild命令后, 手动调用context.stop(child)杀掉所有的孩子
  *
    child3 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-687932161]
    child2 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child2#1728817696]
    child4 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child4#1609964682]
    child0 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child0#432593745]
    child1 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child1#1110544223]
    [WARN] [08/23/2017 17:53:14.597] [MyActorLifeCycle-akka.actor.default-dispatcher-3] [akka.tcp://MyActorLifeCycle@127.0.0.1:61647/system/cluster/core/daemon/downingProvider] Don't use auto-down feature of Akka Cluster in production. See 'Auto-downing (DO NOT USE)' section of Akka Cluster documentation.
    child0 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child0#432593745]
    child1 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child1#1110544223]
    child4 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child4#1609964682]
    child2 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child2#1728817696]
    child3 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-687932161]
    [INFO] [08/23/2017 17:53:14.620] [MyActorLifeCycle-akka.actor.default-dispatcher-3] [akka://MyActorLifeCycle/user/parent/child2] Message [java.lang.String] from Actor[akka://MyActorLifeCycle/user/parent#1874765250] to Actor[akka://MyActorLifeCycle/user/parent/child2#1728817696] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.
    child0 has died
    child4 has died
    child1 has died
    child3 has died
    child2 has died

  注意: 由于所有的孩子actor都已经死掉, 所以发送Kill命令时, 会出现actor不存在. 所以会出现dead letters:

    [INFO] [08/23/2017 17:53:14.620] [MyActorLifeCycle-akka.actor.default-dispatcher-3] [akka://MyActorLifeCycle/user/parent/child2] Message [java.lang.String] from Actor[akka://MyActorLifeCycle/user/parent#1874765250] to Actor[akka://MyActorLifeCycle/user/parent/child2#1728817696] was not delivered. [1] dead letters encountered. This logging can be turned off or adjusted with configuration settings 'akka.log-dead-letters' and 'akka.log-dead-letters-during-shutdown'.

  */
object TestDeathWatchChildActorManualStop {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent3Actor], "parent")

    actor ! "stopchild" // kill all child

    actor ! "kill" // child kill himself, but this is a dead letter

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent3Actor extends Actor {
  override def preStart() {
    for(i <- 0 until 5) {
      context.watch(context.actorOf(Props(new Child3Actor("child" + i)), "child" + i))
    }
  }

  val random = new Random()

  def receive = {
    // 当子类被停止后, 它会发送Terminated消息给父类处理
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case "stopchild" =>
      context.children.foreach(context.stop(_))
    case msg@_ =>
      // 随机发送一条消息给子类, 所有的子类处理任意消息都会模拟抛出一个异常
      val children = context.children.toList
      val index = random.nextInt(5)
      val randomChild = children(index)
      randomChild ! msg
  }
}

class Child3Actor(actorName: String = "child") extends Actor {
  def receive = {
    case "kill" =>
      println("receive kill command, going to kill myself @@" + self)
      self ! Kill
    case _ =>
      throw new Exception(s"$actorName exception")
  }

  override def preStart() {
    println(s"$actorName pre start..." + " @@" + self)
    super.preStart()
  }

  override def postStop(): Unit = {
    println(s"$actorName post stop..." + " @@" + self)
    super.postStop()
  }

  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println(s"$actorName pre restart due to " + throwable.getMessage +
      ", receive msg: " + message + " @@" + self)
    super.preRestart(throwable, message)
  }

  override def postRestart(throwable: Throwable) = {
    println(s"$actorName post restart" + " @@" + self)
    super.postRestart(throwable)
  }
}

