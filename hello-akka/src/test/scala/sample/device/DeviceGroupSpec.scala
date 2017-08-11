package sample.device

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class DeviceGroupSpec(_system: ActorSystem) extends TestKit(_system: ActorSystem)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {
  def this() = this(ActorSystem("iot-system-spec"))

  override def afterAll(): Unit = shutdown(system)

  "A deviceGroup actor" should "be able to register a device actor and check that the device is working as expected" in {
    val probe: TestProbe = TestProbe()
    val groupActor: ActorRef = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(1))
    val deviceActor1: ActorRef = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group", deviceId = "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(2))
    val deviceActor2: ActorRef = probe.lastSender

    deviceActor1 should !==(deviceActor2)
    //
    // Check that the device actors are working as expected
    //

    //
    //  Action : Record Temperature
    //
    deviceActor1.tell(Device.RecordTemperature(requestId = 3, value = 48.5), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 3))

    //
    //  Action : Read Temperature
    //
    deviceActor1.tell(Device.ReadTemperature(requestId = 4), probe.ref)
    val response1 = probe.expectMsgType[Device.RespondTemperature]
    response1.requestId should ===(4)
    response1.value should ===(Some(48.5))

    //
    //  Action : Record Temperature
    //
    deviceActor2.tell(Device.RecordTemperature(requestId = 5, value = 42.5), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 5))

    //
    //  Action: Read Temperature
    //
    deviceActor2.tell(Device.ReadTemperature(requestId = 6), probe.ref)
    probe.expectMsg(Device.RespondTemperature(requestId = 6, value = Some(42.5)))
  }

  "A deviceGroup actor" should "ignore requests for wrong groupId" in {
    val probe: TestProbe = TestProbe()
    val groupActor: ActorRef = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "wrongGroup", "device1"), probe.ref)
    probe.expectNoMsg(500.milliseconds)
  }

  "A deviceGroup actor" should "return the same actor for the same deviceId" in {
    val probe: TestProbe = TestProbe()
    val groupActor: ActorRef = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(1))
    val deviceActor1: ActorRef = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(2))
    val deviceActor2: ActorRef = probe.lastSender

    deviceActor1 should ===(deviceActor2)
  }

  "A deviceGroup actor" should "be able to list active devices" in {
    val probe: TestProbe = TestProbe()
    val groupActor: ActorRef = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(1))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group", deviceId = "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(2))

    groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 3), probe.ref)
    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 3, Set("device1", "device2")))
  }

  "A deviceGroup" should "be able to list active devices after one shuts down" in {
    val probe: TestProbe = TestProbe()
    val groupActor: ActorRef = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(1))
    val deviceToShutDown: ActorRef = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group", deviceId = "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(2))

    groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 3), probe.ref)
    probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 3, devices = Set("device1", "device2")))

    probe.watch(deviceToShutDown: ActorRef)
    deviceToShutDown ! PoisonPill
    probe.expectTerminated(deviceToShutDown: ActorRef)

    // Using awaitAssert to retry because it might take longer for the groupActor
    // to see the Terminated message, that order is undefined
    probe.awaitAssert {
      groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 4), probe.ref)
      probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 4, devices = Set("device2")))
    }
  }

  "A deviceGroup" should "be able to collect temperatures from all active devices" in {
    val probe = TestProbe()
    val groupActor = system.actorOf(DeviceGroup.props("group"))

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device1"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(1))
    val deviceActor1 = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group", deviceId = "device2"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(2))
    val deviceActor2 = probe.lastSender

    groupActor.tell(DeviceManager.RequestTrackDevice(requestId = 3, groupId = "group", deviceId = "device3"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(3))
    val deviceActor3 = probe.lastSender

    // Check that the device actors are working
    deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
    deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
    // No temperature for device3

    groupActor.tell(DeviceGroup.RequestAllTemperatures(requestId = 0), probe.ref)
    probe.expectMsg(
      DeviceGroup.RespondAllTemperatures(
        requestId = 0,
        temperatures = Map(
          "device1" -> DeviceGroup.Temperature(1.0),
          "device2" -> DeviceGroup.Temperature(2.0),
          "device3" -> DeviceGroup.TemperatureNotAvailable)))
  }
}
