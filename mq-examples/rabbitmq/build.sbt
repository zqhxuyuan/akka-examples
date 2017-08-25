name := "rabbitmq"

version := "1.0"

scalaVersion := "2.11.7"

lazy val akkaVersion = "2.5.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,

  "com.rabbitmq" % "amqp-client" % "4.2.0"
)