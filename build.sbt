import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.libraryDependencySchemes
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-financials-frontend"
ThisBuild / majorVersion := 0
val currentScalaVersion = "2.13.18"

scalacOptions ++= Seq(
  "-feature",
  "-Wconf:src=target/.*:silent")

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(playSettings *)
  .settings(scalaSettings *)
  .settings(scalaVersion := currentScalaVersion)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    retrieveManaged := true
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(defaultSettings() *)
  .settings(
    Test / Keys.fork := true,
    Test / javaOptions += "-Dlogger.resource=logback-test.xml",
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    PlayKeys.playDefaultPort := 9092
  )
  .settings(
    Keys.fork := false
  )

lazy val it = project
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings().head)
  .enablePlugins(play.sbt.PlayScala)
  .settings(
    publish / skip := true
  )
  .settings(scalaVersion := currentScalaVersion)
  .settings(
    testForkedParallel := true
  )
  .settings(libraryDependencies ++= AppDependencies.it)

addCommandAlias("compileAll", "compile ; test:compile ; it/test:compile")
