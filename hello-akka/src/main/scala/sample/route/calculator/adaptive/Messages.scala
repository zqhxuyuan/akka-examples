package sample.route.calculator.adaptive

import akka.routing.ConsistentHashingRouter._

/**
  * http://www.cnblogs.com/tiger-xc/p/7110661.html
  */
object Messages {
  class MathOps(hashKey: String) extends Serializable with ConsistentHashable {
    override def consistentHashKey: Any = hashKey
  }
  case class Add(x: Int, y: Int) extends MathOps("adder")
  case class Sub(x: Int, y: Int) extends MathOps("substractor")
  case class Mul(x: Int, y: Int) extends MathOps("multiplier")
  case class Div(x: Int, y: Int) extends MathOps("divider")

}