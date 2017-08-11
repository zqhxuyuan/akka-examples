package sample.device

import akka.actor.{Actor, ActorLogging, Props}

// 设备, 包括所属的设备组编号和设备编号
object Device {
  def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

  // 读取设备的温度
  final case class ReadTemperature(requestId: Long)

  // 返回设备的温度
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  // 记录设备的温度
  final case class RecordTemperature(requestId: Long, value: Double)

  // 完成记录设备的温度
  final case class TemperatureRecorded(requestId: Long)

}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {

  import Device.{ReadTemperature, RecordTemperature, RespondTemperature, TemperatureRecorded}
  import DeviceManager.{DeviceRegistered, RequestTrackDevice}

  var lastTemperatureReading: Option[Double] = None

  override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)

  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {

    case RequestTrackDevice(requestId, `groupId`, `deviceId`) =>
      sender() ! DeviceRegistered(requestId)

    case RequestTrackDevice(requestId, groupId, deviceId) =>
      log.warning(s"Ignoring TrackDevice request for {}-{} using $requestId. This actor is responsible for {}-{}.", groupId, deviceId, this.groupId, this.deviceId)

    case RecordTemperature(requestId, value) =>
      log.info("Recorded temperature reading {} with {}", value, requestId)
      lastTemperatureReading = Some(value)
      sender() ! TemperatureRecorded(requestId)

    case ReadTemperature(requestId) =>
      sender() ! RespondTemperature(requestId, lastTemperatureReading)

  }
}
