package sample.calculator

object Messages {
  sealed trait MathOps
  case class Num(dnum: Double) extends MathOps
  case class Add(dnum: Double) extends MathOps
  case class Sub(dnum: Double) extends MathOps
  case class Mul(dnum: Double) extends MathOps
  case class Div(dnum: Double) extends MathOps

  sealed trait CalcOps
  case object Clear extends CalcOps
  case object GetResult extends CalcOps
}