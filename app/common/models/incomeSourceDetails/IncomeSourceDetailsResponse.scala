/*
 * Copyright 2023 HM Revenue & Customs
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

package common.models.incomeSourceDetails

import common.services.DateServiceInterface
import play.api.libs.json.{Format, JsValue, Json, OFormat}
import play.api.{Logger, Logging}

sealed trait IncomeSourceDetailsResponse {
  def toJson: JsValue
}


case class ChooseSoleTraderAddressUserAnswer(
                                              addressLine1: Option[String],
                                              addressLine2: Option[String],
                                              addressLine3: Option[String],
                                              addressLine4: Option[String],
                                              postcode: Option[String],
                                              countryCode: Option[String],
                                              newAddress: Boolean
                                            )

object ChooseSoleTraderAddressUserAnswer {
  implicit val format: OFormat[ChooseSoleTraderAddressUserAnswer] = Json.format[ChooseSoleTraderAddressUserAnswer]
}

case class IncomeSourceDetailsModel(
                                     nino: String,
                                     mtdbsa: String,
                                     yearOfMigration: Option[String],
                                     businesses: List[BusinessDetailsModel],
                                     properties: List[PropertyDetailsModel],
                                     channel: String = "Customer-led"
                                   ) extends IncomeSourceDetailsResponse with Logging {

  val hasPropertyIncome: Boolean = properties.nonEmpty
  val hasBusinessIncome: Boolean = businesses.nonEmpty
  val hasAnyIncomeSources: Boolean = hasBusinessIncome || hasPropertyIncome
  
  override def toJson: JsValue = Json.toJson(this)

  def orderedTaxYearsByAccountingPeriods(implicit dateService: DateServiceInterface): List[Int] = {
    startingTaxYear match {
      case Some(startTaxYear) =>
        Logger("application").debug(s"[IncomeSourceDetailsModel][orderedTaxYearsByAccountingPeriods] startTaxYear: $startTaxYear, endTaxYear: ${dateService.getCurrentTaxYearEnd}")
        (startTaxYear to dateService.getCurrentTaxYearEnd).toList
      case None =>
        Logger("application").error("[IncomeSourceDetailsModel][orderedTaxYearsByAccountingPeriods] No income source start date found returning empty list")
        List.empty
    }
  }

  def startingTaxYear: Option[Int] = {
    Logger("application").debug(s"[IncomeSourceDetailsModel][startingTaxYear] Businesses firstAccountingPeriodEndDate:${businesses.flatMap(_.firstAccountingPeriodEndDate)}, properties firstAccountingPeriodEndDate: ${properties.flatMap(_.firstAccountingPeriodEndDate)}")
    (businesses.flatMap(_.firstAccountingPeriodEndDate) ++ properties.flatMap(_.firstAccountingPeriodEndDate))
      .map(_.getYear).sortWith(_ < _).headOption
  }

  def orderedTaxYearsByYearOfMigration(implicit dateService: DateServiceInterface): List[Int] = {
    val taxYears = yearOfMigration.map(year => (year.toInt to dateService.getCurrentTaxYearEnd).toList).getOrElse(orderedTaxYearsByAccountingPeriods)
    Logger("application").debug(s"Tax years list = $taxYears")
    taxYears
  }
}

case class IncomeSourceDetailsError(status: Int, reason: String) extends IncomeSourceDetailsResponse {
  override def toJson: JsValue = Json.toJson(this)
}

object IncomeSourceDetailsModel {
  implicit val format: Format[IncomeSourceDetailsModel] = Json.format[IncomeSourceDetailsModel]
}

object IncomeSourceDetailsError {
  implicit val format: Format[IncomeSourceDetailsError] = Json.format[IncomeSourceDetailsError]
}
