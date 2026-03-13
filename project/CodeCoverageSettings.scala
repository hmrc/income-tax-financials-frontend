import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "controllers.*Reverse.*",
    "Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "models.data.*",
    "filters.*",
    "handlers.*",
    "components.*",
    "standardError.*",
    "views.html.*",
    "appConfig.*",
    "controlllers.feedback.*",
    "appConfig.*",
    "com.*",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 100,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
