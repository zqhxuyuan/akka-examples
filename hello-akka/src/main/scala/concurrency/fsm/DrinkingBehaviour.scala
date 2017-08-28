package concurrency.fsm

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object DrinkingBehaviour {
  // Internal message indicating that the blood alcohol level has changed
  case class LevelChanged(level: Float)
  // Outbound messages to tell their person how we're feeling
  case object FeelingSober // 清醒
  case object FeelingTipsy // 喝醉
  case object FeelingLikeZaphod

  // Factory method to instantiate it with the production timer resolution
  def apply(drinker: ActorRef) = new DrinkingBehaviour(drinker) with DrinkingResolution
}

trait DrinkingResolution {
  import scala.util.Random
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration =
    Random.nextInt(300).seconds
}


trait DrinkingProvider {
  def newDrinkingBehaviour(drinker: ActorRef): Props = Props(DrinkingBehaviour(drinker))
}

class DrinkingBehaviour(drinker: ActorRef) extends Actor{
  this: DrinkingResolution =>
  import DrinkingBehaviour._

  // Stores the current blood alcohol level
  var currentLevel = 0f

  // Just provides shorter access to the scheduler
  val scheduler = context.system.scheduler

  // As time passes our Pilot sobers up.  This scheduler keeps that happening
  val sobering = scheduler.schedule(initialSobering, soberingInterval, self, LevelChanged(-0.0001f))

  // Don't forget to stop your timer when the Actor shuts down
  override def postStop() {
    sobering.cancel()
  }

  // We've got to start the ball rolling with a single drink
  override def preStart() {
    drink()
  }

  // The call to drink() is going to schedule a single event
  // to self that will increase the blood alcohol level by a
  // small amount.  It's OK if we don't cancel this one -
  // only one message is going to the Dead Letter Office
  def drink() = scheduler.scheduleOnce(drinkInterval(), self, LevelChanged(0.005f))

  def receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel + amount).max(0f)
      // Tell our drinker how we're feeling.  It gets more
      // exciting when we start feeling like Zaphod himself,
      // but at that point he stops drinking and lets the sobering timer make him feel better.
      drinker ! (if (currentLevel <= 0.01) {
            drink()
            FeelingSober
          } else if (currentLevel <= 0.03) {
            drink()
            FeelingTipsy
          }
          else FeelingLikeZaphod
        )
  }
}
