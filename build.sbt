import xerial.sbt.Sonatype.sonatypeCentralHost

val scala3Version    = "3.7.3"
val zioVersion       = "2.1.21"
val zioSchemaVersion = "1.7.5"
val zioJsonVersion   = "0.7.3"
usePgpKeyHex("2F64727A87F1BCF42FD307DD8582C4F16659A7D6")

lazy val root = project
  .in(file("."))
  .settings(
    name                 := "schemanator",
    description          := "A library for generating JSON schemas from ZIO Schema",
    licenses             := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    organizationName     := "russwyte",
    organization         := "io.github.russwyte",
    organizationHomepage := Some(url("https://github.com/russwyte")),
    scalaVersion         := scala3Version,
    homepage             := Some(url("https://github.com/russwyte/schemanator")),
    licenses             := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        id = "russwyte",
        name = "Russ White",
        email = "356303+russwyte@users.noreply.github.com",
        url = url("https://github.com/russwyte"),
      )
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/russwyte/schemanator"),
        "scm:git@github.com:russwyte/schemanator.git",
      )
    ),
    publishMavenStyle      := true,
    pomIncludeRepository   := { _ => false },
    sonatypeCredentialHost := sonatypeCentralHost,
    publishTo              := sonatypePublishToBundle.value,
    versionScheme          := Some("early-semver"),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"                   % zioVersion,
      "dev.zio" %% "zio-schema"            % zioSchemaVersion,
      "dev.zio" %% "zio-schema-derivation" % zioSchemaVersion,
      "dev.zio" %% "zio-schema-json"       % zioSchemaVersion,
      "dev.zio" %% "zio-json"              % zioJsonVersion,
      "dev.zio" %% "zio-test"              % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt"          % zioVersion % Test,
    ),
  )
