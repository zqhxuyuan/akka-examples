import sbt._
import Keys._

object AkkaExamples extends Build {
  lazy val helloAkka = Project(id = "hello-akka", base = file("hello-akka"))
}