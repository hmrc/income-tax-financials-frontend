/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package common.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait ExternalRedirectHelper {

  val servicesConfig: ServicesConfig
  val config: Configuration
  
  lazy val hubBaseUrl: String = servicesConfig.getString("income-tax-view-change-frontend.baseUrl")
  lazy val hubAgentBaseUrl: String = s"${hubBaseUrl}/agents"
  
  lazy val individualHomeUrl: String =
    hubBaseUrl

  lazy val individualHomeUrlWithOrigin: Option[String] => String = origin =>
    origin.fold(hubBaseUrl)(o => s"$hubBaseUrl?origin=$o")

  lazy val homePageUrl: String = {
    individualHomeUrl
  }


  lazy val agentHomeUrl: String =
    hubAgentBaseUrl
    
  def homePageUrl(isAgent: Boolean): String = if (isAgent) agentHomeUrl else individualHomeUrl

  lazy val enterClientsUTRUrl: String = s"$hubAgentBaseUrl/client-utr"

  lazy val confirmClientUTRUrl: String =
    s"$hubAgentBaseUrl/confirm-client-details"
  
  //Obligation routes
  
  lazy val obligationsBaseUrl: String = servicesConfig.getString("income-tax-obligations-frontend.baseUrl")
  lazy val obligationsAgentBaseUrl: String = s"$obligationsBaseUrl/agents"
  
  lazy val obligationsWaitToSignUpIndividualUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsBaseUrl/access-service-from-next-tax-year"
    else
      s"$hubBaseUrl/access-service-from-next-tax-year"

  lazy val obligationsWaitToSignUpAgentUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsAgentBaseUrl/view-client-from-next-tax-year"
    else
      s"$hubAgentBaseUrl/view-client-from-next-tax-year"

  lazy val obligationsNextUpdatesIndividualUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsBaseUrl/submission-deadlines"
    else
      s"$hubBaseUrl/submission-deadlines"

  lazy val obligationsNextUpdatesAgentUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsAgentBaseUrl/submission-deadlines"
    else
      s"$hubAgentBaseUrl/submission-deadlines"

  def obligationsNextUpdatesUrl(isAgent: Boolean, newObligationsEnabled: Boolean): String = {
    if (isAgent)
      obligationsNextUpdatesAgentUrl(newObligationsEnabled)
    else
      obligationsNextUpdatesIndividualUrl(newObligationsEnabled)
  }

  lazy val obligationsReportingFrequencyIndividualUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsBaseUrl/reporting-frequency"
    else
      s"$hubBaseUrl/reporting-frequency"

  lazy val obligationsReportingFrequencyAgentUrl: Boolean => String = newObligationsEnabled =>
    if (newObligationsEnabled)
      s"$obligationsAgentBaseUrl/reporting-frequency"
    else
      s"$hubAgentBaseUrl/reporting-frequency"


  def obligationsReportingFrequencyUrl(isAgent: Boolean, newObligationsEnabled: Boolean): String = {
    if (isAgent)
      obligationsReportingFrequencyAgentUrl(newObligationsEnabled)
    else
      obligationsReportingFrequencyIndividualUrl(newObligationsEnabled)
  }

  //Business Details routes
  lazy val businessDetailsBaseUrl: String = servicesConfig.getString("income-tax-business-details-frontend.baseUrl")
  lazy val businessDetailsAgentBaseUrl: String = s"$businessDetailsBaseUrl/agents"

  lazy val businessDetailsManageBusinessesIndividualUrl: Boolean => String = businessDetailsFrontendEnabled =>
    if (businessDetailsFrontendEnabled)
      s"$businessDetailsBaseUrl/manage-your-businesses"
    else
      s"$hubBaseUrl/manage-your-businesses"

  lazy val businessDetailsManageBusinessesAgentUrl: Boolean => String = businessDetailsFrontendEnabled =>
    if (businessDetailsFrontendEnabled)
      s"$businessDetailsAgentBaseUrl/manage-your-businesses"
    else
      s"$hubAgentBaseUrl/manage-your-businesses"

  def manageBusinessesUrl(isAgent: Boolean, businessDetailsFrontendEnabled: Boolean): String =
    if (isAgent)
      businessDetailsManageBusinessesAgentUrl(businessDetailsFrontendEnabled)
    else
      businessDetailsManageBusinessesIndividualUrl(businessDetailsFrontendEnabled)

  //Returns routes

  lazy val returnsBaseUrl: String = servicesConfig.getString("income-tax-returns-frontend.baseUrl")
  lazy val returnsAgentBaseUrl: String = s"$returnsBaseUrl/agents"


  lazy val returnsTaxYearsIndividualUrl: Boolean => String = returnsFrontendEnabled =>
    if (returnsFrontendEnabled)
      s"$returnsBaseUrl/tax-years"
    else
      s"$hubBaseUrl/tax-years"

  lazy val returnsTaxYearsAgentUrl: Boolean => String = returnsFrontendEnabled =>
    if (returnsFrontendEnabled)
      s"$returnsAgentBaseUrl/tax-years"
    else
      s"$hubAgentBaseUrl/tax-years"

  def returnsTaxYearsUrl(isAgent: Boolean, returnsFrontendEnabled: Boolean): String =
    if (isAgent)
      returnsTaxYearsAgentUrl(returnsFrontendEnabled)
    else
      returnsTaxYearsIndividualUrl(returnsFrontendEnabled)

  def returnsTaxYearSummaryIndividualUrl(taxYear: Int, origin: Option[String] = None,
                                         fragment: Option[String] = None, returnsFrontendEnabled: Boolean): String = {
    val baseUri = if (returnsFrontendEnabled) {
      s"$returnsBaseUrl/tax-year-summary/$taxYear"
    } else {
      s"$hubBaseUrl/tax-year-summary/$taxYear"
    }
    val baseUriWithOptOrigin = origin.fold(baseUri)(o => s"$baseUri?origin=$o")
    fragment.fold(baseUriWithOptOrigin)(f => s"$baseUriWithOptOrigin#$f")
  }

  def returnsTaxYearSummaryAgentUrl(taxYear: Int, fragment: Option[String] = None, returnsFrontendEnabled: Boolean): String = {
    val baseUri = if (returnsFrontendEnabled) {
      s"$returnsAgentBaseUrl/tax-year-summary/$taxYear"
    } else {
      s"$hubAgentBaseUrl/tax-year-summary/$taxYear"
    }
    fragment.fold(baseUri)(f => s"$baseUri#$f")
  }

}
