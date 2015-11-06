lazy val h2Version = "1.4.190"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtWeb)
  .enablePlugins(JooqCodegen)
  .settings(
    name := "hello-scalikejdbc",
    version := "0.1",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-Xexperimental"
    ),
    resolvers ++= Seq(
      Opts.resolver.sonatypeReleases,
      Opts.resolver.sonatypeSnapshots,
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
    ),
    libraryDependencies ++= Seq(
      jdbc,
      "com.github.kxbmap"      %% "jooqs-play24"       % "0.1.0-SNAPSHOT",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0",
      "com.h2database"         %  "h2"                 % h2Version,
      "org.json4s"             %% "json4s-ext"         % "3.3.0",
      "com.github.tototoshi"   %% "play-json4s-native" % "0.4.2",
      specs2 % "test"
    ),
    routesGenerator := InjectedRoutesGenerator,
    initialCommands := """
      import models._
      import org.jooq._
      import org.jooq.impl.DSL
      import jooqs.syntax._
      Class.forName("org.h2.Driver")
      val db = jooqs.db.Database("jdbc:h2:./db/playapp", "sa", "")
      val (p, c, s, ps) = (Programmer.p, Company.c, Skill.s, ProgrammerSkill.ps)
      implicit val session = db.getSession()
    """,
    cleanupCommands := """
      session.close()
      db.shutdown()
    """,
    jooqCodegen <<= jooqCodegen.dependsOn(flywayMigrate in migration),
    jooqCodegenConfigFile := Some(file("db") / "jooq-codegen.xml"),
    libraryDependencies += "com.h2database" % "h2" % h2Version % "jooq"
  )

lazy val migration = project.settings(
  name := "hello-scalikejdbc-migration",
  version := "0.1",
  scalaVersion := "2.11.7",
  flywaySettings,
  flywayUrl := "jdbc:h2:./db/playapp",
  flywayUser := "sa",
  flywaySchemas := Seq("PUBLIC"),
  flywayLocations := Seq("db/migration/default"),
  libraryDependencies += "com.h2database" % "h2" % h2Version
)
