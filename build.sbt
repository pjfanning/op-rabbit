import java.util.Properties

val json4sVersion = "3.6.11"
val circeVersion = "0.13.0"
val akkaVersion = "2.6.13"
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
  scalaVersion := "2.13.4",
  crossScalaVersions := Seq("2.12.12", "2.13.5"),
  libraryDependencies ++= Seq(
    "com.chuusai" %%  "shapeless" % "2.3.3",
    "com.typesafe" % "config" % "1.4.1",
    "com.newmotion" %% "akka-rabbitmq" % "6.0.0",
    "com.rabbitmq" % "amqp-client" % "5.11.0",
    "org.slf4j" % "slf4j-api" % "1.7.30",
    "com.spingo" %% "scoped-fixtures" % "2.0.0" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "org.scalatest" %% "scalatest" % "3.2.6" % Test,
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion % Test
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
    </developers>
  },
  autoAPIMappings := true // sbt-unidoc setting

)

// removed `akka-stream` temporarily
lazy val `op-rabbit` = (project in file(".")).
  enablePlugins(ScalaUnidocPlugin).
  settings(commonSettings: _*).
  settings(
    description := "The opinionated Rabbit-MQ plugin",
    name := "op-rabbit").
  dependsOn(core).
  aggregate(core, `play-json`, airbrake, json4s, `spray-json`, circe)


lazy val core = (project in file("./core")).
  enablePlugins(spray.boilerplate.BoilerplatePlugin).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-core"
  )

//lazy val demo = (project in file("./demo")).
//  settings(commonSettings: _*).
//  settings(
//    libraryDependencies ++= Seq(
//      "ch.qos.logback" % "logback-classic" % "1.2.3",
//      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion)).
//  settings(
//    name := "op-rabbit-demo"
//  ).
//  dependsOn(
//    `play-json`, `akka-stream`)



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
    libraryDependencies += "io.spray" %% "spray-json" % "1.3.5").
  dependsOn(core)

lazy val airbrake = (project in file("./addons/airbrake/")).
  settings(commonSettings: _*).
  settings(
    name := "op-rabbit-airbrake",
    libraryDependencies += "io.airbrake" % "airbrake-java" % "2.2.8").
  dependsOn(core)

//lazy val `akka-stream` = (project in file("./addons/akka-stream")).
//  settings(commonSettings: _*).
//  settings(
//    name := "op-rabbit-akka-stream",
//    libraryDependencies ++= Seq(
//      "com.timcharper"    %% "acked-streams" % "2.1.1",
//      "com.typesafe.akka" %% "akka-stream" % akkaVersion),
//    unmanagedResourceDirectories in Test ++= Seq(
//      file(".").getAbsoluteFile / "core" / "src" / "test" / "resources"),
//    unmanagedSourceDirectories in Test ++= Seq(
//      file(".").getAbsoluteFile / "core" / "src" / "test" / "scala" / "com" / "spingo" / "op_rabbit" / "helpers")).
//  dependsOn(core)

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
