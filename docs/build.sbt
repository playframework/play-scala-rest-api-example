

resolvers += // for paradox-theme-lightbend
  "bintray-typesafe-internal-maven-releases" at "https://dl.bintray.com/typesafe/internal-maven-releases/"

// https://mvnrepository.com/artifact/com.lightbend.paradox/paradox-theme-generic
libraryDependencies += "com.lightbend.paradox" % "paradox-theme-generic" % "0.2.0"

paradoxTheme := Some("com.lightbend.paradox" % "paradox-theme-generic" % "0.2.0")