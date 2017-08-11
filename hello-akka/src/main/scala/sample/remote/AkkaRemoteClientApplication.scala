package sample.remote

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import com.alibaba.fastjson.JSONObject
import com.typesafe.config.ConfigFactory
import sample._

import _root_.scala.language.postfixOps
import _root_.scala.util.Random

class ClientActor extends Actor with ActorLogging {

  // akka.<protocol>://<actor system>@<hostname>:<port>/<actor path>
  val path = "akka.tcp://remote-system@127.0.0.1:2552/user/remoteActor"
  val remoteServerRef = context.actorSelection(path)

  @volatile var connected = false
  @volatile var stop = false

  def receive = {
    case Start => {
      send(Start)
      if(!connected) {
        connected = true
        log.info("Actor connected: " + this)
      }
    }
    case Stop => {
      send(Stop)
      stop = true
      connected = false
    }
    case header: Header => send(header)
    case hb: Heartbeat => sendWithCheck(hb)
    case pkt: Packet => sendWithCheck(pkt)
    case cmd: Shutdown => send(cmd)
    case (seq, result) => log.info("RESULT: seq=" + seq + ", result=" + result)
    case m => log.info("Unknown message: " + m)
  }

  private def sendWithCheck(cmd: Serializable): Unit = {
    while(!connected) {
      Thread.sleep(100)
      log.info("Wait to be connected...")
    }
    if(!stop) {
      send(cmd)
    } else {
      log.warning("Actor has stopped!")
    }
  }

  private def send(cmd: Serializable): Unit = {
    log.info("Send command to server: " + cmd)
    try {
      remoteServerRef ! cmd
    } catch {
      case e: Exception => {
        connected = false
        log.info("Try to connect by sending Start command...")
        send(Start)
      }
    }
  }

}

object AkkaClientApplication extends App {

  val system = ActorSystem("client-system", ConfigFactory.load().getConfig("MyRemoteClientSideActor"))
  val log = system.log
  val clientActor = system.actorOf(Props[ClientActor], "clientActor")
  @volatile var running = true
  val hbInterval = 1000

  lazy val hbWorker = createHBWorker

  /**
    * create heartbeat worker thread
    */
  def createHBWorker: Thread = {
    new Thread("HB-WORKER") {
      override def run(): Unit = {
        while(running) {
          clientActor ! Heartbeat("HB", 39264)
          Thread.sleep(hbInterval)
        }
      }
    }
  }

  def format(timestamp: Long, format: String): String = {
    val df = new SimpleDateFormat(format)
    df.format(new Date(timestamp))
  }

  def createPacket(packet: Map[String, _]): JSONObject = {
    val pkt = new JSONObject()
    packet.foreach(p => pkt.put(p._1, p._2))
    pkt
  }

  val ID = new AtomicLong(90760000)
  def nextTxID: Long = {
    ID.incrementAndGet()
  }

  clientActor ! Start
  Thread.sleep(2000)

  clientActor ! Header("HEADER", 20, encrypted=false)
  Thread.sleep(2000)

  hbWorker.start

  // send some packets
  val DT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
  val r = Random
  val packetCount = 100
  val serviceProviders = Seq("CMCC", "AKBBC", "OLE")
  val payServiceProvicers = Seq("PayPal", "CMB", "ICBC", "ZMB", "XXB")

  def nextProvider(seq: Seq[String]): String = {
    seq(r.nextInt(seq.size))
  }

  val startWhen = System.currentTimeMillis()
  for(i <- 0 until packetCount) {
    val pkt = createPacket(Map[String, Any](
      "txid" -> nextTxID,
      "pvid" -> nextProvider(serviceProviders),
      "txtm" -> format(System.currentTimeMillis(), DT_FORMAT),
      "payp" -> nextProvider(payServiceProvicers),
      "amount" -> 1000 * r.nextFloat()))
    clientActor ! Packet("PKT", System.currentTimeMillis, pkt.toString)
  }
  val finishWhen = System.currentTimeMillis()
  log.info("FINISH: timeTaken=" + (finishWhen - startWhen) + ", avg=" + packetCount/(finishWhen - startWhen))

  Thread.sleep(2000)

  // ask remote actor to shutdown
  val waitSecs = hbInterval
  clientActor ! Shutdown(waitSecs)

  running = false
  while(hbWorker.isAlive) {
    log.info("Wait heartbeat worker to exit...")
    Thread.sleep(300)
  }
  system.terminate()
}