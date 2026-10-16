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

import common.helpers.ComponentSpecBase
import common.helpers.WiremockHelper
import common.helpers.servicemocks.SessionDataStub
import common.models.sessionData.SessionDataGetResponse.{SessionDataGetSuccess, SessionDataNotFound, SessionDataUnexpectedResponse}
import common.testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSaUtr}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}

class GetSessionDataConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  private lazy val connector: GetSessionDataConnector = app.injector.instanceOf[GetSessionDataConnector]

  "GetSessionDataConnector.getSessionData" should {

    "return the session data when the upstream responds with 200" in {
      SessionDataStub.stubGetSessionDataResponseSuccess()

      val result = connector.getSessionData().futureValue

      result shouldBe Right(SessionDataGetSuccess(testMtditid, testNino, testSaUtr, "sessionId"))
      WiremockHelper.verifyGet("/income-tax-session-data/")
    }

    "return SessionDataUnexpectedResponse when a 200 response contains invalid session JSON" in {
      WiremockHelper.stubGet(
        "/income-tax-session-data/",
        OK,
        s"""{"mtditid":"$testMtditid","utr":"$testSaUtr","sessionId":"sessionId"}"""
      )

      val result = connector.getSessionData().futureValue

      result match {
        case Left(_: SessionDataUnexpectedResponse) => succeed
        case other => fail(s"Expected SessionDataUnexpectedResponse, got $other")
      }
      WiremockHelper.verifyGet("/income-tax-session-data/")
    }

    "return SessionDataNotFound when the upstream responds with 404" in {
      SessionDataStub.stubGetSessionDataResponseNotFound()

      val result = connector.getSessionData().futureValue

      result shouldBe Left(SessionDataNotFound(s"No user session was found. status: $NOT_FOUND"))
      WiremockHelper.verifyGet("/income-tax-session-data/")
    }

    "return SessionDataUnexpectedResponse when the upstream responds with a non-200 and non-404 status" in {
      WiremockHelper.stubGet("/income-tax-session-data/", INTERNAL_SERVER_ERROR, "")

      val result = connector.getSessionData().futureValue

      result shouldBe Left(SessionDataUnexpectedResponse(s"User session could not be retrieved. status: $INTERNAL_SERVER_ERROR"))
      WiremockHelper.verifyGet("/income-tax-session-data/")
    }
  }
}
