name := "akka-analytics"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies ++= Seq(
  "com.github.krasserm" %% "akka-analytics-cassandra" % "0.3.1",
  "com.github.krasserm" %% "akka-analytics-kafka" % "0.3.1"
)