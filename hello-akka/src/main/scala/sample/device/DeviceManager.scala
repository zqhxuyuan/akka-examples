package sample.device

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object DeviceManager {
  def props(): Props = Props(new DeviceManager)

  final case class RequestTrackDevice(requestId: Long, groupId: String, deviceId: String)

  final case class DeviceRegistered(requestId: Long)

  final case class RequestGroupList(requestId: Long)

  final case class ReplyGroupList(requestId: Long, groups: Map[String, ActorRef])

}

class DeviceManager extends Actor with ActorLogging {

  import DeviceManager.{ReplyGroupList, RequestGroupList, RequestTrackDevice}

  var groupIdToActor: Map[String, ActorRef] = Map.empty[String, ActorRef]
  var actorToGroupId: Map[ActorRef, String] = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("DeviceManager started")

  override def postStop(): Unit = log.info("DeviceManager stopped")

  override def receive: Receive = {
    case trackMsg@RequestTrackDevice(_, groupId, _) =>
      groupIdToActor.get(groupId: String) match {
        case Some(ref: ActorRef) =>
          ref forward trackMsg
        case None =>
          log.info("Creating device group actor for {}", groupId)
          val groupActor: ActorRef = context.actorOf(DeviceGroup.props(groupId), s"group-${trackMsg.groupId}")
          context.watch(groupActor)
          groupActor forward trackMsg
          groupIdToActor += groupId -> groupActor
          actorToGroupId += groupActor -> groupId
      }

    case RequestGroupList(requestId: Long) =>
      sender() ! ReplyGroupList(requestId: Long, groupIdToActor: Map[String, ActorRef])


    case Terminated(groupActor: ActorRef) =>
      val groupId = actorToGroupId(groupActor: ActorRef)
      log.info("Group actor for {} has been terminated", groupId)
      actorToGroupId -= groupActor
      groupIdToActor -= groupId
  }
}