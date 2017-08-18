package sample.lifecycle

import akka.actor._

import scala.concurrent.duration._
import scala.util.Random
import akka.pattern._
import akka.util.Timeout
import scala.concurrent._

/**
  * Created by zhengqh on 17/8/14.
  *
  * http://www.cnblogs.com/tiger-xc/p/6898180.html
  */
// 厨师
object Chef {
  sealed trait Order  //消息类型
  case object MakeSpecial extends Order  //烹制特饮
  class ChefBusy(msg: String) extends Exception(msg)  //异常类型
  def props = Props[Chef]
}

class Chef extends Actor with ActorLogging {
  import Chef._

  log.info("Chef says: I am ready to work ...")   //构建成功信息
  //内部状态
  var currentSpecial: Cafe.Coffee = Cafe.Original
  var chefBusy: Boolean = false

  val specials = Map(0 -> Cafe.Original,1 -> Cafe.Espresso, 2 -> Cafe.Cappuccino)

  override def receive: Receive = {
    case MakeSpecial => {
      if ((Random.nextInt(6) % 6) == 0) {  //任意产生异常 2/6
        log.info("Chef is busy ...")
        chefBusy = true
        throw new ChefBusy("Busy!")
      }
      else {
        currentSpecial = randomSpecial     //选出当前特饮
        log.info(s"Chef says: Current special is ${currentSpecial.toString}.")

        sender() ! currentSpecial
      }
    }
  }
  def randomSpecial = specials(Random.nextInt(specials.size)) //选出当前特饮

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"Restarting Chef for ${reason.getMessage}...")
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    log.info(s"Restarted Chef for ${reason.getMessage}.")
    context.parent ! BackoffSupervisor.Reset

    super.postRestart(reason)
  }

  override def postStop(): Unit = {
    log.info("Stopped Chef.")
    super.postStop()
  }
}

//厨房, Kitchen只是Chef的Backoff监管，没有任何其它功能
class Kitchen extends Actor with ActorLogging {
  override def receive: Receive = {
    //context.children.size == 1，就是chef。 直接把所有消息转发到Chef
    case msg@_ =>  //注意，无法使用Chef ？因为sender不明
      context.children foreach ( chef => chef forward msg)
  }

  override def postStop(): Unit = {
    log.info("Kitchen close!")
    super.postStop()
  }
}

object Kitchen {
  //指定的异常处理策略
  val kitchenDecider: PartialFunction[Throwable, SupervisorStrategy.Directive] = {
    case _: Chef.ChefBusy => SupervisorStrategy.Restart
  }
  def kitchenProps: Props = {  //定义BackoffSupervisor strategy
  val option = Backoff.onFailure(Chef.props,"chef",1 seconds, 5 seconds, 0.0)
      .withManualReset
      .withSupervisorStrategy {
        OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds) {
          kitchenDecider.orElse(SupervisorStrategy.defaultDecider)
        }
      }
    BackoffSupervisor.props(option)
  }
}

// 打印机
object ReceiptPrinter {
  case class PrintReceipt(sendTo: ActorRef, receipt: Cafe.Receipt)  //print command
  class PaperJamException extends Exception
  def props = Props[ReceiptPrinter]
}

class ReceiptPrinter extends Actor with ActorLogging {
  import ReceiptPrinter._
  var paperJammed: Boolean = false
  override def receive: Receive = {
    case PrintReceipt(customer, receipt) =>    //打印收据并发送给顾客
      if ((Random.nextInt(6) % 6) == 0) {
        log.info("Printer jammed paper ...")
        paperJammed = true
        throw new PaperJamException
      } else {
        log.info(s"Printing receipt $receipt and sending to ${customer.path.name}")
        customer ! receipt
      }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"Restarting ReceiptPrinter for ${reason.getMessage}...")
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    log.info(s"Started ReceiptPrinter for ${reason.getMessage}.")
    super.postRestart(reason)
  }

  override def postStop(): Unit = {
    log.info("Stopped ReceiptPrinter.")
    super.postStop()
  }
}

// 收银员
object Cashier {
  case class RingRegister(cup: Cafe.Coffee, customer: ActorRef)  //收款并出具收据

  def props(kitchen: ActorRef) = Props(classOf[Cashier],kitchen)
}

class Cashier(kitchen: ActorRef) extends Actor with ActorLogging {
  import Cashier._
  import ReceiptPrinter._

  context.watch(kitchen)   //监视厨房。如果打烊了就关门歇业
  val printer = context.actorOf(ReceiptPrinter.props,"printer")
  //打印机卡纸后重启策略
  def cashierDecider: PartialFunction[Throwable,SupervisorStrategy.Directive] = {
    case _: PaperJamException => SupervisorStrategy.Restart
  }
  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 5 seconds){
      cashierDecider.orElse(SupervisorStrategy.defaultDecider)
    }
  val menu = Map[Cafe.Coffee,Double](Cafe.Original -> 5.50,
    Cafe.Cappuccino -> 12.95, Cafe.Espresso -> 11.80)

  override def receive: Receive = {
    case RingRegister(coffee, customer) => //收款并出具收据
      log.info(s"Producing receipt for a cup of ${coffee.toString}...")
      val amt = menu(coffee)    //计价
    val rcpt = Cafe.Receipt(coffee.toString,amt)
      printer ! PrintReceipt(customer,rcpt)  //打印收据。可能出现卡纸异常
      sender() ! Cafe.Sold(rcpt)  //通知Cafe销售成功  sender === Cafe
    case Terminated(_) =>
      log.info("Cashier says: Oh, kitchen is closed. Let's make the end of day!")
      context.system.terminate()   //厨房打烊，停止营业。
  }
}

// 咖啡厅
object Cafe {
  sealed trait Coffee  //咖啡种类
  case object Original extends Coffee
  case object Espresso extends Coffee
  case object Cappuccino extends Coffee

  case class Receipt(item: String, amt: Double)

  sealed trait Routine
  case object PlaceOrder extends Routine
  case class Sold(receipt: Receipt) extends Routine
}

class Cafe extends Actor with ActorLogging {
  import Cafe._
  import Cashier._

  import context.dispatcher
  implicit val timeout = Timeout(1 seconds)

  var totalAmount: Double = 0.0

  val kitchen = context.actorOf(Kitchen.kitchenProps,"kitchen")
  //Chef可能重启，但path不变。必须直接用chef ? msg，否则经Kitchen转发无法获取正确的sender
  val chef = context.actorSelection("/user/cafe/kitchen/chef")

  val cashier = context.actorOf(Cashier.props(kitchen),"cashier")

  var customer: ActorRef = _     //当前客户

  override def receive: Receive = {

    case Sold(rcpt) =>
      totalAmount += rcpt.amt
      log.info(s"Today's sales is up to $totalAmount")
      customer ! Customer.OrderServed(rcpt)   //send him the order
      if (totalAmount > 100.00) {
        log.info("Asking kichen to clean up ...")
        context.stop(kitchen)
      }
    case PlaceOrder =>
      customer = sender()     //send coffee to this customer
      (for {
        item <- (chef ? Chef.MakeSpecial).mapTo[Coffee]
        sales <- (cashier ? RingRegister(item,sender())).mapTo[Sold]
      } yield(Sold(sales.receipt))).mapTo[Sold]
        .recover {
          case _: AskTimeoutException => Customer.ComebackLater
        }.pipeTo(self)   //send receipt to be added to totalAmount

  }
}

// 顾客
object Customer {
  sealed trait CustomerOrder
  case object OrderSpecial extends CustomerOrder
  case class OrderServed(rcpt: Cafe.Receipt) extends CustomerOrder
  case object ComebackLater extends CustomerOrder
  def props(cafe: ActorRef) = Props(new Customer(cafe))
}

class Customer(cafe: ActorRef) extends Actor with ActorLogging {
  import Customer._
  import context.dispatcher
  override def receive: Receive = {
    case OrderSpecial =>
      log.info("Customer place an order ...")
      cafe ! Cafe.PlaceOrder
    case OrderServed(rcpt) =>
      log.info(s"Customer says: Oh my! got my order ${rcpt.item} for ${rcpt.amt}")
    case ComebackLater =>
      log.info("Customer is not so happy! says: I will be back later!")
      context.system.scheduler.scheduleOnce(1 seconds){cafe ! Cafe.PlaceOrder}
  }
}

// 应用程序
object MyCafe extends App {
  import Cafe._
  import Customer._
  import scala.concurrent.ExecutionContext.Implicits.global
  val cafeSys = ActorSystem("cafeSystem")
  val cafe = cafeSys.actorOf(Props[Cafe],"cafe")
  val customer = cafeSys.actorOf(Customer.props(cafe),"customer")

  cafeSys.scheduler.schedule(1 second, 1 second, customer, OrderSpecial)
}