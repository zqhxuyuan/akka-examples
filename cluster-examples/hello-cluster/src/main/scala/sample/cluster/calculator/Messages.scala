package sample.cluster.calculator

/**
  * http://www.cnblogs.com/tiger-xc/p/7110661.html
  */
object Messages {
  sealed trait MathOps
  case class Add(x: Int, y: Int) extends MathOps
  case class Sub(x: Int, y: Int) extends MathOps
  case class Mul(x: Int, y: Int) extends MathOps
  case class Div(x: Int, y: Int) extends MathOps

  sealed trait ClusterMsg
  case class RegisterBackendActor(role: String) extends ClusterMsg

}