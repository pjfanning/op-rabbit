import java.util.Properties

val json4sVersion = "4.0.5"
val circeVersion = "0.14.2"
val pekkoVersion = "1.0.0"
val playVersion = "2.9.2"

val appProperties = {
  val prop = new Properties()
  IO.load(prop, new File("project/version.properties"))
  prop
}

val assertNoApplicationConf = taskKey[Unit]("Makes sure application.conf isn't packaged")

val commonSettings = Seq(
  organization := "com.github.pjfanning",
  version := appProperties.getProperty("version"),
  scalaVersion := "2.13.11",
  crossScalaVersions := Seq("2.12.18", "2.13.11"),
  libraryDependencies ++= Seq(
    "com.chuusai" %%  "shapeless" % "2.3.9",
    "com.typesafe" % "config" % "1.4.2",
    "com.github.pjfanning" %% "pekko-rabbitmq" % "7.0.0",
    "com.rabbitmq" % "amqp-client" % "5.14.3",
    "org.slf4j" % "slf4j-api" % "1.7.36",
    "com.spingo" %% "scoped-fixtures" % "2.0.0" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.12" % Test,
    "org.scalatest" %% "scalatest" % "3.2.16" % Test,
    "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
    "org.apache.pekko" %% "pekko-testkit" % pekkoVersion % Test,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion % Test
  ),
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/pjfanning/op-rabbit")),
  pomExtra := {
    <scm>
      <url>https://github.com/pjfanning/op-rabbit</url>
      <connection>scm:git:git@github.com:pjfanning/op-rabbit.git</connection>
    </scm>
    <developers>
      <developer>
        <id>timcharper</id>
        <name>Tim Harper</name>
        <url>http://spingo.com</url>
      </developer>
      <developer>
        <id>pjfanning</id>
        <name>PJ Fanning</name>
        <url>https://github.com/pjfanning</url>
      </developer>
    </developers>
  },
  autoAPIMappings := true // sbt-unidoc setting

)

lazy val `op-rabbit` = (project in file(".")).
  enablePlugins(ScalaUnidocPlugin).
  settings(commonSettings: _*).
  settings(
    description := "The opinionated Rabbit-MQ plugin",
    name := "op-rabbit").
  dependsOn(core).
  aggregate(core, `play-json`, airbrake, `pekko-stream`, json4s, `spray-json`, circe)


lazy val core = (project in file("./core")).
  enablePlugins(spray.boilerplate.BoilerplatePlugin).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-core"
  )

lazy val json4s = (project in file("./addons/json4s")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-json4s",
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-ast"     % json4sVersion,
      "org.json4s" %% "json4s-core"    % json4sVersion,
      "org.json4s" %% "json4s-jackson" % json4sVersion % "provided",
      "org.json4s" %% "json4s-native"  % json4sVersion % "provided")).
  dependsOn(core)

lazy val `play-json` = (project in file("./addons/play-json")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-play-json",
    libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion).
  dependsOn(core)

lazy val `spray-json` = (project in file("./addons/spray-json")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-spray-json",
    libraryDependencies += "io.spray" %% "spray-json" % "1.3.6").
  dependsOn(core)

lazy val airbrake = (project in file("./addons/airbrake/")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-airbrake",
    libraryDependencies += "io.airbrake" % "airbrake-java" % "2.2.8").
  dependsOn(core)

lazy val `pekko-stream` = (project in file("./addons/pekko-stream")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-pekko-stream",
    libraryDependencies ++= Seq(
      "com.github.pjfanning" %% "acked-streams" % "1.0.0",
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion),
    Test / unmanagedResourceDirectories ++= Seq(
      file(".").getAbsoluteFile / "core" / "src" / "test" / "resources"),
    Test / unmanagedSourceDirectories ++= Seq(
      file(".").getAbsoluteFile / "core" / "src" / "test" / "scala" / "com" / "github" / "pjfanning" / "op_rabbit" / "helpers")).
  dependsOn(core)

lazy val circe = (project in file("./addons/circe")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion
    )).
  dependsOn(core)
