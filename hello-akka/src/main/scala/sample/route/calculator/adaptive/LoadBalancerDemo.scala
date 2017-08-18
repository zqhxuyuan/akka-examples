package sample.route.calculator.adaptive

import Messages._

object LoadBalancerDemo extends App {

  BackEnd.create(2551)   //seed-node
  BackEnd.create(0)      //backend node
  BackEnd.create(0)
  BackEnd.create(0)
  BackEnd.create(0)
  BackEnd.create(0)

  Thread.sleep(2000)

  FrontEnd.create

  Thread.sleep(2000)

  val router = FrontEnd.getRouter

  router ! Add(10,3)
  router ! Mul(3,7)
  router ! Div(8,2)
  router ! Sub(45, 3)
  router ! Div(8,0)

  Thread.sleep(2000)

  router ! Add(10,3)
  router ! Mul(3,7)
  router ! Div(8,2)
  router ! Sub(45, 3)
  router ! Div(8,0)

}
