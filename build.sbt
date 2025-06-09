val scala3Version = "3.7.0"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
lazy val AkkaVersion = "2.10.6"
fork := true
lazy val akkaGroup = "com.typesafe.akka"
lazy val root = project
  .in(file("."))
  .settings(
    name := "assignment-03",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.1.1" % Test,
      akkaGroup %% "akka-actor-typed" % AkkaVersion,
      akkaGroup %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
      "ch.qos.logback" % "logback-classic" % "1.5.18"
    )
  )

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
