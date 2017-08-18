import sbt._
import Keys._

object AkkaExamples extends Build {
  lazy val helloAkka = Project(id = "hello-akka", base = file("hello-akka"))
  lazy val akkaAnalytics = Project(id = "akka-analytics", base = file("akka-analytics"))
  lazy val diExamples = Project(id = "di-examples", base = file("di-examples"))
}