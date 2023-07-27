
val _scalaVersion = "3.3.0"

organization := "io.github.makingthematrix"
sonatypeProfileName := "io.github.makingthematrix"

name := "inject"
homepage := Some(url("https://github.com/makingthematrix/inject"))
licenses := Seq("GPL 3.0" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html"))
ThisBuild / scalaVersion := _scalaVersion
ThisBuild / versionScheme := Some("semver-spec")
Test / scalaVersion := _scalaVersion
ThisBuild / version := "1.0.0"

val standardOptions = Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding",
  "utf8"
)

val scala3Options = Seq(
  "-explain",
  "-Ysafe-init",
  "-Ycheck-all-patmat",
  "-Wunused:imports"
)

publishMavenStyle := true
Test / publishArtifact := false
pomIncludeRepository := { _ => false }
ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"

scmInfo := Some(
  ScmInfo(
    url("https://github.com/makingthematrix/inject"),
    "scm:git:git@github.com:makingthematrix/inject.git"
  )
)

developers := List(
  Developer(
    "makingthematrix",
    "Maciej Gorywoda",
    "makingthematrix@protonmail.com",
    url("https://github.com/makingthematrix"))
)

resolvers ++=
  Resolver.sonatypeOssRepos("releases") ++
    Resolver.sonatypeOssRepos("public") ++
    Seq(Resolver.mavenLocal)

publishMavenStyle := true

publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
publishM2Configuration := publishM2Configuration.value.withOverwrite(true)

lazy val root = (project in file("."))
  .settings(
    name := "inject",
    libraryDependencies ++= Seq(
      //Test dependencies
      "org.scalameta" %% "munit" % "0.7.29" % "test"
    ),
    scalacOptions ++= standardOptions ++ scala3Options
  )

testFrameworks += new TestFramework("munit.Framework")

exportJars := true
Compile / packageBin / packageOptions +=
  Package.ManifestAttributes("Automatic-Module-Name" -> "inject")

usePgpKeyHex(sys.env.getOrElse("PGP_KEY_HEX", ""))
