package concurrency.actor

/**
  * Created by zhengqh on 17/8/18.
  */
object MySequencedObject {
  def doSomething(withThis: String): Unit = {
    println(withThis)
  }
}

object MySequencedApp {
  def main(args: Array[String]) {
    MySequencedObject.doSomething("With this")
    println("It's your turn now!")
  }
}
