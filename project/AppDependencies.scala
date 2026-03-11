import sbt.Keys.libraryDependencies
import sbt._
import play.sbt.PlayImport.ws
import play.sbt.PlayImport.caffeine

object AppDependencies {

  val bootstrapPlayVersion = "10.5.0"
  val playPartialsVersion = "10.2.0"
  val playFrontendHMRCVersion = "12.22.0"
  val catsVersion = "2.13.0"
  val scalaTestPlusVersion = "7.0.2"
  val jsoupVersion = "1.22.1"
  val mockitoVersion = "5.21.0"
  val scalaMockVersion = "7.5.3"
  val wiremockVersion = "3.0.0-beta-7"
  val hmrcMongoVersion = "2.11.0"
  val playVersion = "play-30"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% s"play-partials-$playVersion" % playPartialsVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % hmrcMongoVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHMRCVersion,
    "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "8.4.0",
    "org.jsoup" % "jsoup" % jsoupVersion
  )

  val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % Test,
    "org.scalamock" %% "scalamock" % scalaMockVersion % Test,
    "org.jsoup" % "jsoup" % jsoupVersion % Test,
    "org.mockito" % "mockito-core" % mockitoVersion % Test,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % Test,
    "org.scalacheck" %% "scalacheck" % "1.19.0" % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % Test,
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootstrapPlayVersion % Test,
    caffeine,
    "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "8.4.0" % Test,
  )

  val it = Seq.empty
}
