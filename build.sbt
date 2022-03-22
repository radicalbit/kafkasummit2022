import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
import sbt.Keys._
import sbt._

addCommandAlias("dockerComposeUpRest", """;set composeFile:="docker-compose-rest.yml";dockerComposeUp""")
addCommandAlias("dockerComposeStopRest", """;set composeFile:="docker-compose-rest.yml";dockerComposeStop""")
addCommandAlias("dockerComposeUpKafka", """;set composeFile:="docker-compose-kafka.yml";dockerComposeUp""")
addCommandAlias("dockerComposeStopKafka", """;set composeFile:="docker-compose-kafka.yml";dockerComposeStop""")

val PureConfigVersion     = "0.16.0"
val HeliconJavaApiVersion = "1.0.0-SNAPSHOT"
val ScalaTestVersion      = "3.2.9"
val Slf4jVersion          = "1.7.30"
val LogbackVersion        = "1.2.3"
val JaninoVersion         = "3.1.3"
val KafkaVersion          = "3.1.0"
val CirceVersion          = "0.14.1"
val SttpVersion           = "3.5.1"

val Dependencies = Seq(
  "com.github.pureconfig"         %% "pureconfig"               % PureConfigVersion,
  "ch.qos.logback"                 % "logback-classic"          % LogbackVersion,
  "org.codehaus.janino"            % "janino"                   % JaninoVersion,
  "org.slf4j"                      % "log4j-over-slf4j"         % Slf4jVersion,
  "org.apache.kafka"              %% "kafka"                    % KafkaVersion,
  "org.apache.kafka"              %% "kafka-streams-scala"      % KafkaVersion,
  "io.circe"                      %% "circe-core"               % CirceVersion,
  "io.circe"                      %% "circe-generic"            % CirceVersion,
  "io.circe"                      %% "circe-parser"             % CirceVersion,
  "com.softwaremill.sttp.client3" %% "core"                     % SttpVersion,
  "com.softwaremill.sttp.client3" %% "circe"                    % SttpVersion,
  "org.scalatest"                 %% "scalatest"                % ScalaTestVersion % Test
)


lazy val macroSettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided
  ),
  scalacOptions ++= Seq("-Ymacro-annotations", "-language:higherKinds"),
  scalacOptions -= "-Xfatal-warnings"
)

lazy val `continuum-data-generator` = (project in file("."))
  .enablePlugins(JavaAppPackaging, BuildInfoPlugin)
  .enablePlugins(DockerPlugin, DockerComposePlugin)
  .settings(
    Docker / packageName := packageName.value,
    Docker / version := version.value,
    Docker / maintainer := organization.value,
    dockerRepository := Some("tools.radicalbit.io"),
    Docker / defaultLinuxInstallLocation := s"/opt/${packageName.value}",
    dockerCommands := Seq(
      Cmd("FROM", "tools.radicalbit.io/service-java-base:1.5"),
      Cmd("WORKDIR", s"/opt/${packageName.value}"),
      Cmd("RUN", "addgroup", "-S", "continuum", "&&", "adduser", "-S", "continuum", "-G", "continuum"),
      Cmd("ADD", "opt", "/opt"),
      ExecCmd("RUN", "chown", "-R", "continuum:continuum", "."),
      Cmd("USER", "continuum"),
      Cmd("CMD", s"bin/${packageName.value}")
    ),
    dockerImageCreationTask := (Docker / publishLocal).value,
    variablesForSubstitution := Map(
      "image_name"      -> s"${dockerRepository.value.get}/${packageName.value}",
      "project_version" -> version.value
    )
  )
  .settings(
    resolvers ++= Seq(
      "confluent" at "https://packages.confluent.io/maven/",
      "jitpack" at "https://jitpack.io"
    ),
    credentials += Credentials(Path.userHome / ".artifactory" / ".credentials"),
    macroSettings
  )
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.radicalbit.continuum",
    buildInfoOptions += BuildInfoOption.ToJson
  )
  .settings(
    organization := "io.radicalbit",
    name := "kafkasummit2022",
    scalaVersion := "2.13.6",
    scalafmtOnCompile := true,
    libraryDependencies ++= Dependencies,
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.0" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
  )
