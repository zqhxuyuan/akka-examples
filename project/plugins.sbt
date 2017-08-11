resolvers += Resolver.url("artifactory", url("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-multi-jvm" % "0.3.11")
