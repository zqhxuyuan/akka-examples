package quickstart

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.rabbitmq.client.{ConnectionFactory, Connection, Channel, QueueingConsumer}

class ReceiverExample {

  def receive(actor: ActorRef): Unit = {
    val factory = new ConnectionFactory
    factory.setHost("localhost")
    val connection = factory.newConnection
    val channel = connection.createChannel
    channel.queueDeclare("fruits", false, false, false, null)
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume("fruits", true, consumer)
    while (true) {
      val delivery = consumer.nextDelivery()
      val payload = new String(delivery.getBody())
      actor ! StringMessage(payload)
    }
  }
}

case class StringMessage(payload: String)

class ReceiverActor extends Actor {
  def receive = {
    case StringMessage(payload) => println(s"receive string message [$payload]")
  }
}

object ReceiverExample {
  def main(args: Array[String]): Unit = {
    val example = new ReceiverExample
    val system = ActorSystem("DefaultSystem")
    val actor = system.actorOf(Props[ReceiverActor], name = "receiver")
    example.receive(actor)
  }
}
