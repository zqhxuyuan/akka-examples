package sample.device

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

// 设备组, 包括一个设备组编号
object DeviceGroup {
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(requestId: Long)

  final case class ReplyDeviceList(requestId: Long, devices: Set[String])

  final case class RequestAllTemperatures(requestId: Long)

  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  // 读取设备温度的几种返回类型
  sealed trait TemperatureReading

  final case class Temperature(value: Double) extends TemperatureReading

  case object TemperatureNotAvailable extends TemperatureReading

  case object DeviceNotAvailable extends TemperatureReading

  case object DeviceTimedOut extends TemperatureReading

}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {

  import DeviceGroup.{ReplyDeviceList, RequestAllTemperatures, RequestDeviceList}
  import DeviceManager.RequestTrackDevice

  var deviceIdToActor: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var actorToDeviceId: Map[ActorRef, String] = Map.empty[ActorRef, String]
  var nextCollectionId: Long = 0L

  override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)

  override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

  override def receive: Receive = {

    case trackMsg@RequestTrackDevice(_, `groupId`, _) =>
      deviceIdToActor.get(trackMsg.deviceId: String) match {
        case Some(deviceActor: ActorRef) =>
          deviceActor forward trackMsg
        case None =>
          log.info("Creating device actor for {}", trackMsg.deviceId: String)
          val deviceActor: ActorRef = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
          context.watch(deviceActor: ActorRef)
          actorToDeviceId += deviceActor -> trackMsg.deviceId
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          deviceActor forward trackMsg
      }

    case RequestTrackDevice(requestId: Long, groupId: String, _: String) =>
      log.warning("Ignoring TrackDevice request for {} reported by {}. This device is responsible for {}.", groupId, requestId, this.groupId)

    case RequestDeviceList(requestId: Long) =>
      sender() ! ReplyDeviceList(requestId: Long, deviceIdToActor.keySet: Set[String])

    case Terminated(deviceActor: ActorRef) =>
      val deviceId = actorToDeviceId(deviceActor: ActorRef)
      log.info("Device actor for {} has been terminated", deviceId)
      actorToDeviceId -= deviceActor
      deviceIdToActor -= deviceId

    case RequestAllTemperatures(requestId: Long) =>
      context.actorOf(DeviceGroupQuery.props(actorToDeviceId = actorToDeviceId, requestId = requestId, requester = sender(), 3.seconds))
  }
}