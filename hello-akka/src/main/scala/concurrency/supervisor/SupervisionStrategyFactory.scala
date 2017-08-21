package concurrency.supervisor

import akka.actor.{SupervisorStrategy, OneForOneStrategy, AllForOneStrategy}
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration.Duration

trait SupervisionStrategyFactory {
  def makeStrategy(
                    maxNrRetries: Int,
                    withinTimeRange: Duration
                  )(decider: Decider): SupervisorStrategy
}

// 一对一. 如果actor失败了, 父actor只针对这个失败的子actor
trait OneForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(
                    maxNrRetries: Int,
                    withinTimeRange: Duration
                  )(decider: Decider): SupervisorStrategy = OneForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}

// 多对一. 如果一个actor失败了, 父actor会针对所有的子actor
trait AllForOneStrategyFactory extends SupervisionStrategyFactory {
  def makeStrategy(
                    maxNrRetries: Int,
                    withinTimeRange: Duration
                  )(decider: Decider): SupervisorStrategy = AllForOneStrategy(maxNrRetries, withinTimeRange)(decider)
}

