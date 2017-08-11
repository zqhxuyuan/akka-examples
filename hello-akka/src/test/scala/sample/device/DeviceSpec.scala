package sample.device

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class DeviceSpec(_system: ActorSystem) extends TestKit(_system: ActorSystem) with Matchers with FlatSpecLike with BeforeAndAfterAll {
  def this() = this(ActorSystem("iot-system-spec"))

  override def afterAll(): Unit = shutdown(system)

  "A device actor" should "reply with an empty reading if no temperature is known" in {
    val probe: TestProbe = TestProbe()
    val deviceActor: ActorRef = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.ReadTemperature(requestId = 42), probe.ref)

    val response: Device.RespondTemperature = probe.expectMsgType[Device.RespondTemperature]
    response.requestId should ===(42)
    response.value should ===(None)
  }

  "A device actor" should "reply with the latest temperature reading" in {
    val probe: TestProbe = TestProbe()
    val deviceActor: ActorRef = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(Device.RecordTemperature(requestId = 1, 24.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 1))

    deviceActor.tell(Device.ReadTemperature(requestId = 2), probe.ref)
    val response1: Device.RespondTemperature = probe.expectMsgType[Device.RespondTemperature]
    response1.requestId should ===(2)
    response1.value should ===(Some(24.0))

    deviceActor.tell(Device.RecordTemperature(requestId = 3, value = 55.0), probe.ref)
    probe.expectMsg(Device.TemperatureRecorded(requestId = 3))

    deviceActor.tell(Device.ReadTemperature(requestId = 4), probe.ref)
    val response2: Device.RespondTemperature = probe.expectMsgType[Device.RespondTemperature]
    response2.requestId should ===(4)
    response2.value should ===(Some(55.0))
  }

  "A device actor" should "reply to registration requests" in {
    val probe: TestProbe = TestProbe()
    val deviceActor: ActorRef = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "group", deviceId = "device"), probe.ref)
    probe.expectMsg(DeviceManager.DeviceRegistered(requestId = 1))
    probe.lastSender should ===(deviceActor)

  }

  "A device actor" should "ignore wrong registration requests" in {
    val probe: TestProbe = TestProbe()
    val deviceActor: ActorRef = system.actorOf(Device.props("group", "device"))

    deviceActor.tell(DeviceManager.RequestTrackDevice(requestId = 1, groupId = "wrongGroup", deviceId = "device"), probe.ref)
    probe.expectNoMsg(500.milliseconds)

    deviceActor.tell(DeviceManager.RequestTrackDevice(requestId = 2, groupId = "group", deviceId = "wrongDevice"), probe.ref)
    probe.expectNoMsg(500.milliseconds)
  }
}
