lazy val h2Version = "1.4.187"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtWeb)
  .enablePlugins(JooqCodegen)
  .settings(
    name := "hello-scalikejdbc",
    version := "0.1",
    scalaVersion := "2.11.7",
    resolvers ++= Seq(
      "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
      "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
    ),
    libraryDependencies ++= Seq(
      jdbc,
      "com.h2database"       %  "h2"                 % h2Version,
      "org.json4s"           %% "json4s-ext"         % "3.2.11",
      "com.github.tototoshi" %% "play-json4s-native" % "0.4.0",
      "org.flywaydb"         %% "flyway-play"        % "2.0.1",
      specs2 % "test"
    ),
    checksums := Nil, // play-json4s-native_2.11-0.4.0.pom: invalid sha1
    routesGenerator := InjectedRoutesGenerator,
    initialCommands := """
      import models._
      import org.jooq._
      import org.jooq.impl.DSL
      Class.forName("org.h2.Driver")
      implicit val __connection = java.sql.DriverManager.getConnection("jdbc:h2:./db/playapp", "sa", "")
      val (p, c, s, ps) = (Programmer.p, Company.c, Skill.s, ProgrammerSkill.ps)
      val ctx = DSL.using(__connection)
    """,
    cleanupCommands := """
      __connection.close()
    """,
    jooqCodegenConfigFile := Some(file("project") / "jooq-codegen.xml"),
    libraryDependencies += "com.h2database" % "h2" % h2Version % "jooq",
    jooqCodegen <<= jooqCodegen.dependsOn(flywayMigrate in migration)
  )
  .dependsOn(migration)

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
