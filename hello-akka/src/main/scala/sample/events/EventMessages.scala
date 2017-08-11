package sample.events

object Registration extends Serializable

trait EventMessage extends Serializable
case class RawNginxRecord(sourceHost: String, line: String) extends EventMessage
case class NginxRecord(sourceHost: String, eventCode: String, line: String) extends EventMessage
case class FilteredRecord(sourceHost: String, eventCode: String, line: String, logDate: String, realIp: String) extends EventMessage