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
    s"$hubBaseUrl/income-tax"

  lazy val individualHomeUrlWithOrigin: Option[String] => String = origin =>
    origin.fold(individualHomeUrl)(o => s"$individualHomeUrl?origin=$o")

  lazy val homePageUrl: String = {
    individualHomeUrl
  }
  
  lazy val agentHomeUrl: String =
    s"$hubAgentBaseUrl/client-income-tax"
    
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
      
  //Business Details routes

  lazy val businessDetailsBaseUrl: String = servicesConfig.getString("income-tax-business-details-frontend.baseUrl")
  lazy val businessDetailsAgentBaseUrl: String = s"$businessDetailsBaseUrl/agents"

  def triggeredMigrationCheckHMRCRecordsUrl(isAgent: Boolean, businessDetailsFrontendEnabled: Boolean): String = {
    if (businessDetailsFrontendEnabled) {
      val baseUri = if (isAgent) businessDetailsAgentBaseUrl else businessDetailsBaseUrl
      s"$baseUri/check-your-active-businesses/hmrc-record"
    } else {
      val baseUri = if (isAgent) hubAgentBaseUrl else hubBaseUrl
      s"$baseUri/check-your-active-businesses/hmrc-record"
    }
  }

  //Returns routes

  lazy val returnsBaseUrl: String = servicesConfig.getString("income-tax-returns-frontend.baseUrl")
  lazy val returnsAgentBaseUrl: String = s"$returnsBaseUrl/agents"

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
