package sample.device

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class DeviceManagerSpec(_system: ActorSystem) extends TestKit(_system: ActorSystem)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("iot-system-spec"))

  override def afterAll(): Unit = shutdown(system)

  "A deviceManager" should "be able to register a group" in {
    val probe: TestProbe = TestProbe()
    val managerActor: ActorRef = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 1))
  }

  "A deviceManager" should "be able to register different groups independently" in {
    val probe: TestProbe = TestProbe()
    val managerActor = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group1", "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 1))
    val registeredGroup1: ActorRef = probe.lastSender

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group2", deviceId = "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 2))
    val registeredGroup2: ActorRef = probe.lastSender

    registeredGroup1 should !==(registeredGroup2)

  }

  "A deviceManager" should "be able to maintain the same list of groups if an existing group tries to be recreated" in {
    val probe: TestProbe = TestProbe()
    val managerActor: ActorRef = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group1", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 1))

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group2", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 2))

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 3, groupId = "group1", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 3))

    managerActor.tell(DeviceManager.RequestGroupList(requestId = 4), probe.ref)

    val deviceGroupList: DeviceManager.ReplyGroupList = probe.expectMsgType[DeviceManager.ReplyGroupList]
    deviceGroupList.requestId should ===(4)
    deviceGroupList.groups.keySet should equal(Set("group1", "group2"))

  }

  "A deviceManager" should "be able to list active groups" in {
    val probe: TestProbe = TestProbe()
    val managerActor: ActorRef = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group1", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(1))

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group2", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(2))

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 3, groupId = "group3", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(3))

    managerActor.tell(DeviceManager.RequestGroupList(requestId = 4), probe.ref)
    val deviceGroupList = probe.expectMsgType[DeviceManager.ReplyGroupList]
    deviceGroupList.requestId should equal(4)
    deviceGroupList.groups.keySet should equal(Set("group1", "group2", "group3"))
  }

  "A deviceManager" should "be able to list active groups after one shuts down" in {
    val probe: TestProbe = TestProbe()
    val managerActor: ActorRef = system.actorOf(DeviceManager.props())

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group1", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 1))

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group2", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 2))

    managerActor.tell(DeviceManager.RequestTrackDevice(requestId = 3, groupId = "group3", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 3))

    managerActor.tell(DeviceManager.RequestGroupList(requestId = 4), probe.ref)
    val deviceGroupList = probe.expectMsgType[DeviceManager.ReplyGroupList]
    deviceGroupList.requestId should equal(4)
    deviceGroupList.groups.keySet should equal(Set("group1", "group2", "group3"))

    val groupToShutDown: ActorRef = deviceGroupList.groups("group1")
    probe.watch(groupToShutDown: ActorRef)
    groupToShutDown ! PoisonPill
    probe.expectTerminated(groupToShutDown: ActorRef)

    // Using awaitAssert to retry because it might take longer for the managerActor
    // to see the Terminated message, that order is undefined
    probe.awaitAssert {
      managerActor.tell(DeviceManager.RequestGroupList(requestId = 5), probe.ref)
      val modifiedDeviceGroupList = probe.expectMsgType[DeviceManager.ReplyGroupList]
      modifiedDeviceGroupList.requestId should equal(5)
      modifiedDeviceGroupList.groups.keySet should equal(Set("group2", "group3"))
    }
  }
}
