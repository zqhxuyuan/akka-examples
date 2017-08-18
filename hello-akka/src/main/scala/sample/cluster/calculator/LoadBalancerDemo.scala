package sample.cluster.calculator

import Messages._
import sample.cluster.calculator.backend.Calculator
import sample.cluster.calculator.frontend.FrontEnd

object LoadBalancerDemo extends App {
  // 前台, seed-nodes, 必须先启动
  FrontEnd.create

  // 后台, worker, 后启动. 启动时向seed-nodes注册
  Calculator.create("adder")

  Calculator.create("substractor")

  Calculator.create("multiplier")

  Calculator.create("divider")

  Thread.sleep(2000)

  val router = FrontEnd.getRouter

  // 不同的计算类型, 路由到不同的worker
  router ! Add(10,3)
  router ! Mul(3,7)
  router ! Div(8,2)
  router ! Sub(45, 3)
  router ! Div(8,0)

  /**
    * 10 + 3 carried out by Actor[akka://calcClusterSystem/user/calculator/calcFunction#69099533] with result=13
    * 3 * 7 carried out by Actor[akka://calcClusterSystem/user/calculator/calcFunction#1894622280] with result=21
    * 8 / 2 carried out by Actor[akka://calcClusterSystem/user/calculator/calcFunction#2038714549] with result=4
    * 45 - 3 carried out by Actor[akka://calcClusterSystem/user/calculator/calcFunction#523227838] with result=42
    * [WARN] [08/15/2017 10:12:07.003] [calcClusterSystem-akka.actor.default-dispatcher-14] [akka://calcClusterSystem/user/calculator/calcFunction] / by zero
    */
}