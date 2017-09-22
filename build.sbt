import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

inThisBuild(
  List(
    scalaVersion := "2.12.3",
    dependencyOverrides := Set(
       "com.google.code.findbugs" % "jsr305" % "3.0.1",
       "com.google.guava" % "guava" % "22.0",
       "com.typesafe.akka" %% "akka-stream" % "2.5.4",
       "com.typesafe.akka" %% "akka-actor" % "2.5.4"
    )
  )
)

libraryDependencies += guice
libraryDependencies += "org.joda" % "joda-convert" % "1.8.3"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "4.11"

libraryDependencies += "io.lemonlabs" %% "scala-uri" % "0.5.0"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test"
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.3.0" % Test

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """play-scala-rest-api-example""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )

// Documentation for this project:
//    sbt "project docs" "~ paradox"
//    open docs/target/paradox/site/index.html
lazy val docs = (project in file("docs")).enablePlugins(ParadoxPlugin).
  settings(
    paradoxProperties += ("download_url" -> "https://example.lightbend.com/v1/download/play-rest-api")
  )
