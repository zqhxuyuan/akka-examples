package concurrency.lifecycle.restartchild

import akka.actor._

class MyActorChildWatch extends Actor {

  override def preStart() {
    val childActor = context.actorOf(Props[MySimpleChildBeWatchedActor], "child")
    context.watch(childActor)
  }

  def receive = {
    // 5
    case Terminated(deadActor) =>
      println(deadActor.path.name + " has died")
      // 6
      self ! "restartchild"
    case "restartchild" =>
      preStart()
      self ! "restartchildren"
    case msg@_ =>
      // 2
      context.children.foreach(child => child forward msg)
  }
}

class MySimpleChildBeWatchedActor extends Actor {
  def receive = {
    // 4
    case "killchild" =>
      // 模拟产生ActorKilledException. 默认的监控策略,针对该异常,会停止actor. 即child会发送Terminated给监管者(parent)
      println("killed self: " + self)
      self ! Kill
    // 7
    case "restartchildren" =>
      println("restarted by parent, now:" + self)
    case _ =>
      // 3
      throw new Exception("child exception")
  }
}

object MyActorChildWatchApp {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycleWithChild")

    val actor = system.actorOf(Props[MyActorChildWatch], "parent")

    // 1
    actor ! "parent msg"

    // 4
    actor ! "killchild"

    Thread.sleep(5000)
    system.terminate()
  }
}