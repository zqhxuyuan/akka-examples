name := "akka-examples"

version := "1.0"

scalaVersion := "2.11.7"

// 最简单的SBT工程, 只有一个工程, 将依赖定义在最外层的build.sbt

//lazy val akkaVersion = "2.5.3"

//libraryDependencies ++= Seq(
//  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
//  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
//  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
//)

// SBT多模块工程, 通过aggregate聚合多个子工程.
// 编译信息定义在Common类中,各个模块的依赖信息定义在Dependencies类中

//lazy val clusterapp = project.
//  settings(Common.settings: _*).
//  settings(libraryDependencies ++= Dependencies.clusterappDependencies)
//
//lazy val root = (project in file(".")).aggregate(
//  clusterapp
//)