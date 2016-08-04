import sbt.Keys._

// Set up two data repositories that will serve us raw data which we have to
// present through a REST API.  These don't know about Play, and can be run
// on their own without including any Play classes.

// Provides post repository to the system.
lazy val postModule = (project in file("modules/post")).enablePlugins(Common)

// Provides comment repository to the system.
lazy val commentModule = (project in file("modules/comment")).enablePlugins(Common)

// The Play project itself, which aggregates everything.
lazy val root = (project in file(".")).enablePlugins(Common, PlayScala)
  .settings(
    name := """rest-api""",
    libraryDependencies ++= Seq(
      // Use Play filters to set up security headers and HSTS.
      filters,

      // A useful URL construction library
      "com.netaporter" %% "scala-uri" % "0.4.14",

      // metrics library (used in PostAction)
      "nl.grons" %% "metrics-scala" % "3.5.4_a2.3",

      // Add in a failsafe library
      "net.jodah" % "failsafe" % "0.9.2",

      // Pull in play-bootstrap for twitter bootstrap form helper styling
      "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3"
      //play-bootstrap adds "org.webjars" % "bootstrap" % "3.3.6",
      //play-bootstrap adds "org.webjars" % "jquery" % "2.2.3"
    )
  ).aggregate(postModule, commentModule)
  .dependsOn(postModule, commentModule)

// Required to set "javaOptions" where system properties are set on another JVM
fork in ThisBuild := true

// Set up the JVM to run on an additional HTTPS port on 9443
javaOptions in ThisBuild ++= Seq("-Dhttps.port=9443") ++ strongHttpsSettings

// These are not required, but are useful defaults to secure HTTPS in Java
lazy val strongHttpsSettings = Seq(
  "-Djava.security.properties=disabledAlgorithms.properties", // should only use TLS 1.2
  "-Djdk.tls.ephemeralDHKeySize=2048", // decent DH key size
  //"-Djavax.net.debug=ssl,handshake", // debugging
  "-Djdk.tls.rejectClientInitiatedRenegotiation=true" // no client downgrade attacks
)
