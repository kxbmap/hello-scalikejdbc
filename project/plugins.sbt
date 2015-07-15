resolvers ++= Seq(
  Opts.resolver.sonatypeReleases,
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)
libraryDependencies += "com.h2database" % "h2" % "1.4.187"

addSbtPlugin("com.typesafe.play" % "sbt-plugin"       % "2.4.2")
addSbtPlugin("com.typesafe.sbt"  % "sbt-coffeescript" % "1.0.0")
addSbtPlugin("com.github.kxbmap" % "sbt-jooq"         % "0.1.0")
addSbtPlugin("org.flywaydb"      % "flyway-sbt"       % "3.2.1")
