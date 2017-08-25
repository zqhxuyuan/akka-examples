package sample

object Start extends Serializable
object Stop extends Serializable

trait Message {
  val id: String
}

case class Shutdown(waitSecs: Int) extends Serializable
case class Heartbeat(id: String, magic:Int) extends Message with Serializable
case class Header(id: String, len: Int, encrypted: Boolean) extends Message with Serializable
case class Packet(id: String, seq: Long, content: String) extends Message with Serializable