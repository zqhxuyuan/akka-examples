import sbt._
import Keys._

object AkkaExamples extends Build {
  lazy val helloScala = Project(id = "hello-scala", base = file("hello-scala"))
  lazy val helloAkka = Project(id = "hello-akka", base = file("hello-akka"))
  lazy val akkaAnalytics = Project(id = "akka-analytics", base = file("akka-analytics"))
  lazy val diExamples = Project(id = "di-examples", base = file("di-examples"))
  lazy val streamExamples = Project(id = "stream-examples", base = file("stream-examples"))

  lazy val helloCluster = Project(id = "cluster-examples-hello", base = file("cluster-examples/hello-cluster"))
  lazy val helloPersist = Project(id = "cluster-examples-persist", base = file("cluster-examples/hello-persist"))
  lazy val clusterExamples = (project in file("cluster-examples")).aggregate(helloCluster, helloPersist)

  lazy val messages = Project(id = "remote-examples-messages", base = file("remote-examples/messages"))//.settings(commonSettings)
  lazy val local = Project(id = "remote-examples-local", base = file("remote-examples/local")).dependsOn(messages)//.settings(commonSettings)
  lazy val remote = Project(id = "remote-examples-remote", base = file("remote-examples/remote")).dependsOn(messages)//.settings(commonSettings)
  lazy val remoteExamples = (project in file("remote-examples")).aggregate(messages,local,remote)

  lazy val akkaRabbitMQ = Project(id = "mq-examples-rabbitmq", base = file("mq-examples/rabbitmq"))
  lazy val mqExamples = (project in file("mq-examples")).aggregate(akkaRabbitMQ)
}