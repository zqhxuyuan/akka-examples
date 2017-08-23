package concurrency.future

import org.scalatest.{MustMatchers, WordSpec, BeforeAndAfterAll}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class MultiExecContextSpec
  extends WordSpec with MustMatchers {
  import scala.math.BigInt

  // 0 1 1 2 3 5 8 13 21 34 55 89 ...
  lazy val fibs: Stream[BigInt] = BigInt(0) #:: BigInt(1) #:: fibs.zip(fibs.tail).map { n => n._1 + n._2 }

  import java.util.concurrent.Executors
  import scala.concurrent.duration._
  import scala.concurrent.{ExecutionContext, Future, Await}

  // We need to create the ExecutionContext, which we can build from
  // an existing ExecutorService from plain ol' java.util.concurrent
  val execService = Executors.newCachedThreadPool()
  implicit val execContext = ExecutionContext.fromExecutorService(execService)

  "Future" should {
    "calculate fibonacci numbers" in {
      // We pass the ExecutionContext to the Future on which it will execute
      val futureFib = Future {fibs.drop(99).head}

      // We then use the Await object's result() funtion to block the current thread
      // until the result is available or 1 second has passed
      val fib = Await.result(futureFib, 1.second)

      // Just make sure it's cool
      fib must be (BigInt("218922995834555169026"))

      // Shut down the ExecutionContext or this thread will never die
      execContext.shutdown()
    }

    "compose two future together" in {
      val future = Future { fibs.drop(28).head }

      val anotherFuture = future.map(result => {
        factorize(result)
      })

      val sideEffect = anotherFuture.andThen {
        case Success(Tuple2(fib, factors)) =>
          println(s"Factors for $fib are ${factors.mkString(", ")}")
        case Failure(e) =>
          println("Something went wrong - " + e)
      }
      // Prints: Factors for 317811 are 1, 3, 13, 29, 39, 87, 381, 377
    }

    "future parallel" in {
      try {
        val fib = Await.result(Future {
          fibs.drop(28).head
        }, 1.seconds)

        val factors = Await.result(Future {
          factorize(fib)
        }, 5.seconds)

        println(s"Factors for $fib are ${factors._2.mkString(", ")}")
      } catch {
        case e => println(s"Something went wrong - $e")
      }
    }

    "single thread promise and future" in {
      import scala.concurrent.Promise

      // Create a Promise
      val promise = Promise[String]()

      // Get the associated Future from that Promise
      val future = promise.future

      // Successfully fulfill the Promise
      promise.success("I always keep my promises!")

      // Extract the value from the Future
      println(future.value)

      // Prints: Some(Success(I always keep my promises!))
    }
  }

  // 求公因式, 即能够整除的序列. 比如317811能够整除381,377,...
  def factorize(num: BigInt): Tuple2[BigInt, Seq[Int]] = {
    import math._
    // 求公因式时,并不需要从1到num,而只需要到num的根号即可
    (num, (1 to floor(sqrt(num.toDouble)).toInt) filter { i => num % i == 0 })
  }

}