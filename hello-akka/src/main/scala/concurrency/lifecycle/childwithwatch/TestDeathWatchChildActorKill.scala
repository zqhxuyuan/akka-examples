package concurrency.lifecycle.childwithwatch

import akka.actor.SupervisorStrategy._
import akka.actor._

import scala.concurrent.duration._
import scala.util.Random

/**
  * stopchild时的消息处理类似, kill才会触发Terminated.
  * 另外, 这里两条消息有可能不是发送给同一个Actor.
  * 并且, 两条消息的处理顺序也不是顺序,而是同时进行.
  *
    child4 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child4#-1045854255]
    child9 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child9#1797861032]
    child5 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child5#1329866565]
    child8 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child8#-1763215613]
    child6 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child6#-1736938090]
    child7 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child7#-1441605659]
    child2 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child2#1569494160]
    child3 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-527602511]
    child0 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child0#-69750984]
    child1 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child1#-448270052]

    [ERROR] [08/23/2017 17:14:28.421] [MyActorLifeCycle-akka.actor.default-dispatcher-15] [akka://MyActorLifeCycle/user/parent/child4] child4 exception
    java.lang.Exception: child4 exception
    child4 pre restart due to child4 exception, receive msg: Some(stopchild) @@Actor[akka://MyActorLifeCycle/user/parent/child4#-1045854255]
    [ERROR] [08/23/2017 17:14:28.423] [MyActorLifeCycle-akka.actor.default-dispatcher-15] [akka://MyActorLifeCycle/user/parent/child2] Kill (akka.actor.ActorKilledException: Kill)
    child4 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child4#-1045854255]
    child2 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child2#1569494160]
    child4 post restart @@Actor[akka://MyActorLifeCycle/user/parent/child4#-1045854255]
    child4 pre start... @@Actor[akka://MyActorLifeCycle/user/parent/child4#-1045854255]
    child2 has died

  child4接收了stop消息, child2接收了kill消息. 所以child4仍然会被restart, 而child2只有两条日志:

    1. child2 post stop
    2. child2 has died

  terminate system:

    child8 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child8#-1763215613]
    child6 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child6#-1736938090]
    child7 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child7#-1441605659]
    child4 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child4#-1045854255]
    child1 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child1#-448270052]
    child5 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child5#1329866565]
    child9 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child9#1797861032]
    child0 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child0#-69750984]
    child3 post stop... @@Actor[akka://MyActorLifeCycle/user/parent/child3#-527602511]

  注意到child2已经死掉了, 所以不会再stop掉child2, 其余child都会被stop掉

  */
object TestDeathWatchChildActorKill {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[Parent2Actor], "parent")

    actor ! "stopchild" // child throw exception

    actor ! "kill" // child kill himself

    Thread.sleep(5000)
    system.terminate()
  }
}

class Parent2Actor extends Actor {
  override def preStart() {
    for(i <- 0 until 10) {
      context.watch(context.actorOf(Props(new Child2Actor("child" + i)), "child" + i))
    }
  }

  val random = new Random()

  def receive = {
    // 当子类被停止后, 它会发送Terminated消息给父类处理
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
    case msg@_ =>
      // 随机发送一条消息给子类, 所有的子类处理任意消息都会模拟抛出一个异常
      val children = context.children.toList
      val index = random.nextInt(10)
      val randomChild = children(index)
      randomChild ! msg
  }
}

class Child2Actor(actorName: String = "child") extends Actor {
  def receive = {
    case "kill" =>
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

