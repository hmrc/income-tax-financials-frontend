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

package common.connectors

import common.config.FrontendAppConfig
import common.models.admin.FeatureSwitch
import play.api.http.Status.OK
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FeatureSwitchConnector @Inject()(val appConfig: FrontendAppConfig,
                                       http: HttpClientV2)(implicit ec: ExecutionContext) extends RawResponseReads{

  def switchesStubBaseUrl: String = {
    s"${appConfig.dynamicStubUrl}/features"
  }

  def getAllSwitches()(implicit headerCarrier: HeaderCarrier): Future[List[FeatureSwitch]] = {

    val url = switchesStubBaseUrl

    http.get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            response.json.as[Seq[FeatureSwitch]].toList
          case _ =>
            throw new RuntimeException(s"Failed to fetch feature switches: ${response.status}")
        }
      }
  }

}