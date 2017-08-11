name := "hello-akka"

version := "1.0"

scalaVersion := "2.11.7"

lazy val akkaVersion = "2.5.3"

lazy val kafkaVersion = "0.8.2.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,

  "org.iq80.leveldb"                    % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"           % "leveldbjni-all"   % "1.8",

  "com.alibaba"        % "fastjson"     % "1.2.32",
  "org.apache.kafka"  %% "kafka"        % kafkaVersion,

  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest"     %% "scalatest"    % "3.0.1" % "test"
)