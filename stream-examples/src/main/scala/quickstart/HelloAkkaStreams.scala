package quickstart

import akka.stream._
import akka.stream.scaladsl._

import akka.{ NotUsed, Done }
import akka.actor.ActorSystem
import akka.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths

/**
  * Created by zhengqh on 17/8/25.
  *
  * http://doc.akka.io/docs/akka/current/scala/stream/stream-quickstart.html
  */
object HelloAkkaStreams {

  def main(args: Array[String]) {
    implicit val system = ActorSystem("QuickStart")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 100)

    // 运行
    source.runForeach(i => println(i))(materializer)

    // 返回Future
    val done: Future[Done] =
      source.runForeach(i => println(i))(materializer)

    // 可重用
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

    val result: Future[IOResult] =
      factorials
        .map(num => ByteString(s"$num\n"))
        .runWith(FileIO.toPath(Paths.get("factorials.txt")))

    // 输入和输出
    factorials.map(_.toString).runWith(lineSink("factorial2.txt"))

    // Time-Based Processing
    factorials
      .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .runForeach(result => println(System.currentTimeMillis() + " : " + result))

    // 结束
    implicit val ec = system.dispatcher
    done.onComplete(_ => system.terminate())
  }


  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
}
