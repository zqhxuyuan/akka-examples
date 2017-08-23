package concurrency.lifecycle.singleactorstartstop

import akka.actor.{Props, ActorSystem, Actor}

/**
  * 增加异常处理, 模拟产生异常: Actor接收一条消息, 直接抛出一个异常
  *
  * 1. pre start
  * 2. pre restart
  * 3. post stop
  * 4. post restart
  * 5. pre start
  *
  * *****************************************
  *
  * Actor Life Cycle when deal with Exception
  *
  * 1. pre start hook
  * 2. create actor action
  * 3. throw exception
  * 4. suspend and restart actor
  *    4.1 pre restart hook
  *    4.2 post stop hook
  *    4.3 restarting actor
  * 5. pre start hook, start actor success
  * 6. destroy actor
  * 7. post stop hook
  *
  * 总结几个hook方法的调用顺序
  *
  * 1) 没有重启,只有启动和停止时:
  *
  * preStart - Started - ... - Stopped - postStop - Death
  *
  * 2) 发生重启时:
  *
  * preRestart - postStop - postRestart - preStart --> Started -- ...
  * 重启前        停止后      重启后         启动前       已经启动
  */
object TestExceptionAndRestart {
  def main(args: Array[String]) {
    val system = ActorSystem("MyActorLifeCycle")
    val actor = system.actorOf(Props[MyActor2], "MyActorLifeCycle")

    actor ! "exception"

    Thread.sleep(5000)
    system.terminate()
  }
}

class MyActor2 extends Actor {
  override def preStart() {
    println("my actor pre start...")
    super.preStart()
  }

  override def postStop(): Unit = {
    println("my actor post stop...")
    super.postStop()
  }

  // 默认preRestart方法会先停止所有孩子, 然后调用postStop()方法
  // 所以自定义的preRestart()方法先于postStop()方法被调用
  override def preRestart(throwable: Throwable, message: Option[Any]): Unit = {
    println("my actor pre restart due to " + message.get)
    super.preRestart(throwable, message)
  }

  // 默认postRestart方法会调用preStart方法, 所以postRestart会紧接着preStart
  override def postRestart(throwable: Throwable) = {
    println("my actor post restart")
    super.postRestart(throwable)
  }

  def receive = {
    case "exception" =>
      throw new Exception("exception")
    case _ =>
  }
}
