resolvers ++= Seq(
  Opts.resolver.sonatypeReleases,
  Opts.resolver.sonatypeSnapshots,
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)
libraryDependencies += "com.h2database" % "h2" % "1.4.190"

addSbtPlugin("com.typesafe.play" % "sbt-plugin"       % "2.4.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-coffeescript" % "1.0.0")
addSbtPlugin("com.github.kxbmap" % "sbt-jooq"         % "0.2.0-SNAPSHOT")
addSbtPlugin("org.flywaydb"      % "flyway-sbt"       % "3.2.1")
