package sample.events

import java.util.Properties

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.cluster.protobuf.msg.ClusterMessages.MemberStatus
import com.alibaba.fastjson.JSONObject
import com.typesafe.config.ConfigFactory
import kafka.producer.{Producer, ProducerConfig, KeyedMessage}

class EventProcessor extends ClusterRoledWorker {

  val topic = "akka_app_events"
  val producer = KakfaUtils.createProcuder

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      // 将processor注册到上游的collector中
      register(member, getProcessorPath)
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up).foreach(register(_, getProcessorPath))
    case UnreachableMember(member) =>
      log.info("Member detected as Unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent => // ignore

    case FilteredRecord(sourceHost, eventCode, line, nginxDate, realIp) => {
      val data = process(eventCode, line, nginxDate, realIp)
      log.info("Processed: data=" + data)
      // 将解析后的消息一JSON字符串的格式，保存到Kafka中
      producer.send(new KeyedMessage[String, String](topic, sourceHost, data.toString))
    }
  }

  def getProcessorPath(member: Member): ActorPath = {
    RootActorPath(member.address) / "user" / "interceptingActor"
  }

  private def process(eventCode: String, line: String, eventDate: String, realIp: String): JSONObject = {
    val data: JSONObject = new JSONObject()
    "[\\?|&]{1}([^=]+)=([^&]+)&".r.findAllMatchIn(line) foreach { m =>
      val key = m.group(1)
      val value = m.group(2)
      data.put(key, value)
    }
    data.put("eventdate", eventDate)
    data.put("realip", realIp)
    data
  }
}

object KakfaUtils {
  // bin/kafka-topics.sh --create -zookeeper zk1:2181,zk2:2181,zk3:2181/data-dept/kafka --replication-factor 2 --partitions 2 --topic app_events
  val props = new Properties()
  val config = Map(
    "metadata.broker.list" -> "192.168.6.55:9092,192.168.6.56:9092,192.168.6.57:9092",
    "serializer.class" -> "kafka.serializer.StringEncoder",
    "producer.type" -> "async"
  )
  config.foreach(entry => props.put(entry._1, entry._2))
  val producerConfig = new ProducerConfig(props)

  def createProcuder() : Producer[String, String] = {
    new Producer[String, String](producerConfig)
  }
}

object EventProcessor extends App {

  // 启动了5个EventProcessor
  Seq("2951","2952", "2953", "2954", "2955") foreach { port =>
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
      .withFallback(ConfigFactory.parseString("akka.cluster.roles = [processor]"))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("event-cluster-system", config)
    val processingActor = system.actorOf(Props[EventProcessor], name = "processingActor")
    system.log.info("Processing Actor: " + processingActor)
  }
}