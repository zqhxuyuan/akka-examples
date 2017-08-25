package quickstart

import com.rabbitmq.client.ConnectionFactory

object SenderExample extends App{

  private val QUEUE_NAME = "fruits"

  val factory = new ConnectionFactory()
  factory.setHost("localhost")
  val conn = factory.newConnection()
  val channel = conn.createChannel()
  channel.queueDeclare(QUEUE_NAME, false, false, false, null)

  val payload = "Hello, world!"
  channel.basicPublish("", QUEUE_NAME, null, payload.getBytes)
}