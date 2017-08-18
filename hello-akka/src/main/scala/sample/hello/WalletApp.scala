package sample.hello

import akka.actor._
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config._

/**
  * http://www.cnblogs.com/tiger-xc/p/6785575.html
  *
  * 从功能上Actor是由实例引用（ActorRef），消息邮箱（Mailbox），内部状态（State），运算行为（Behavior），
  * 子类下属（Child-Actor），监管策略（Supervision/Monitoring）几部分组成。
  * Actor的物理结构由ActorRef、Actor Instance（runtime实例）、Mailbox、dispatcher（运算器）组成。
  *
  * 1、ActorRef：Akka系统是一个树形层级式的结构，每个节点由一个Actor代表。每一个Actor在结构中都可以用
  *             一个路径（ActorPath）来代表它在系统结构里的位置。我们可以重复用这个路径来构建Actor，
  *             但每次构建都会产生新的ActorRef。所以ActorRef是唯一的，代表了某个路径指向位置上的一个
  *             运行时的Actor实例，我们只能用ActorRef来向Actor发送消息
  * 2、Mailbox：可以说成是一个运算指令队列（command queque）。Actor从外部接收的消息都是先存放在Mailbox里的。
  *             系统默认Mailbox中无限数量的消息是按时间顺序排列的，但用户可以按照具体需要定制Mailbox，
  *             比如有限容量信箱、按消息优先排序信箱等。
  * 3、Behavior：简单来说就是对Mailbox里消息的反应方式。Mailbox中临时存放了从外界传来的指令，如何运算这些指令、
  *             产生什么结果都是由这些指令的运算函数来确定。所以这些函数的功能就代表着Actor的行为模式。
  *             Actor的运算行为可以通过become来替换默认的receive函数，用unbecome来恢复默认行为。
  * 4、State：Actor内部状态，由一组变量值表示。当前内部状态即行为函数最后一次运算所产生的变量值
  */

// 钱包
object Wallet {
  sealed trait WalletMsg
  case object ZipUp extends WalletMsg    //锁钱包
  case object UnZip extends WalletMsg    //开钱包
  case class PutIn(amt: Double) extends WalletMsg   //存入
  case class DrawOut(amt: Double) extends WalletMsg //取出
  case object CheckBalance extends WalletMsg  //查看余额

  def props = Props(new Wallet)
}

// Mailbox的优先级
class PriorityMailbox(settings: ActorSystem.Settings, config: Config)
  extends UnboundedPriorityMailbox (
    PriorityGenerator {
      case Wallet.ZipUp => 0
      case Wallet.UnZip => 0
      case Wallet.PutIn(_) => 0
      case Wallet.DrawOut(_) => 2
      case Wallet.CheckBalance => 4
      case PoisonPill => 4
      case otherwise => 4
    }
  )

class Wallet extends Actor {
  import Wallet._
  var balance: Double = 0
  var zipped: Boolean = true

  override def receive: Receive = {
    case ZipUp =>
      zipped = true
      println("Zipping up wallet.")
    case UnZip =>
      zipped = false
      println("Unzipping wallet.")
    case PutIn(amt) =>
      if (zipped) {
        self ! UnZip         //无论如何都要把钱存入
        self ! PutIn(amt)
      } else {
        balance += amt
        println(s"$amt put-in wallet.")
      }
    case DrawOut(amt) =>
      if (zipped)  //如果钱包没有打开就算了
        println("Wallet zipped, Cannot draw out!")
      else if ((balance - amt) < 0)
        println(s"$amt is too much, not enough in wallet!")
      else {
        balance -= amt
        println(s"$amt drawn out of wallet.")
      }
    case CheckBalance =>
      println(s"You have $balance in your wallet.")
  }
}

object WalletApp extends App {
  val system = ActorSystem("actor101-demo",ConfigFactory.load())
  val wallet = system.actorOf(
    Wallet.props.withDispatcher("prio-dispatcher"), "mean-wallet")

  wallet ! Wallet.UnZip
  wallet ! Wallet.PutIn(10.50)
  wallet ! Wallet.PutIn(20.30)
  wallet ! Wallet.DrawOut(10.00)
  wallet ! Wallet.ZipUp
  wallet ! Wallet.PutIn(100.00)
  wallet ! Wallet.CheckBalance

  Thread.sleep(1000)
  system.terminate()
}