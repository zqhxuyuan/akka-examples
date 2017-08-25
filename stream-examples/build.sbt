name := "stream-examples"

version := "1.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.5.4"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.17"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.14",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
  "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.2",
  "com.google.protobuf" % "protobuf-java" % "2.5.0",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.2"
)