val scala3Version = "3.7.0"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")
lazy val AkkaVersion = "2.10.6"
fork := true

lazy val root = project
  .in(file("."))
  .settings(
    name := "git status",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test)
  )


// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/


