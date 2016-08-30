import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"

// The Play project itself, which aggregates everything.
lazy val root = (project in file(".")).enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """play-rest-api""",
    libraryDependencies ++= Seq(
      // A useful URL construction library
      "com.netaporter" %% "scala-uri" % "0.4.14",

      // Use scala-guice
      "net.codingwell" %% "scala-guice" % "4.1.0"
    )
  )
