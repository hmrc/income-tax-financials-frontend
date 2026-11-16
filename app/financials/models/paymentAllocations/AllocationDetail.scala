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

package financials.models.paymentAllocations

import common.exceptions.MissingFieldException
import common.models.core.AccountingPeriodModel
import financials.models.{ChargeType, FinancialDetail}
import play.api.Logging
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class AllocationDetail(
                             transactionId: Option[String],
                             from: Option[LocalDate],
                             to: Option[LocalDate],
                             fallbackTaxYear: Option[Int],
                             chargeType: Option[String],
                             mainType: Option[String],
                             amount: Option[BigDecimal],
                             clearedAmount: Option[BigDecimal],
                             chargeReference: Option[String],
                             mainTransaction: Option[String] = None
                           ) extends Logging {

  def getPaymentAllocationKeyInPaymentAllocations: String = {
    val typesKey =
      if (mainTransaction.contains(ChargeType.overRepaymentCharge)) FinancialDetail.getOverRepaymentMessageKey(chargeType)
      else FinancialDetail.getMessageKeyByTypes(mainType, chargeType)

    typesKey
      .map(key => s"paymentAllocation.paymentAllocations.$key")
      .getOrElse {
        logger.error(s"Non-matching document/charge found with main charge: $mainType, main transaction: $mainTransaction and sub-charge: $chargeType")
        ""
      }
  }

  def getTaxYear: Int = {
    getTaxYearOpt.getOrElse(throw new Exception("Missing tax year"))
  }

  def getTaxYearOpt: Option[Int] = {
    to.map(AccountingPeriodModel.determineTaxYearFromPeriodEnd)
      .orElse(fallbackTaxYear)
  }

  def transactionIdActual: String = transactionId.getOrElse(throw MissingFieldException("Document ID"))

  def amountActual: BigDecimal = amount.getOrElse(throw MissingFieldException("Payment Amount"))
}

object AllocationDetail {
  implicit val format: Format[AllocationDetail] = Json.format[AllocationDetail]
}