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

package financials.models

import common.exceptions.MissingFieldException
import common.testUtils.TestSupport
import financials.enums.ChargeType.{CGT, SL}
import financials.models.paymentAllocations.AllocationDetail
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDate

class AllocationDetailSpec extends TestSupport with Matchers {

  private val localToDateOpt = Some(LocalDate.parse("2019-01-04"))

  def allocationDetails(mainType: Option[String], chargeType: Option[String], to: Option[LocalDate]): AllocationDetail = {
    AllocationDetail(Some("id"),
      localToDateOpt,
      to, None, chargeType, mainType, Some(10000.0), Some(5000.0), Some("chargeReference1"))
  }

  def overRepaymentAllocation(chargeType: Option[String], mainType: Option[String] = Some("ITSA Over-repayment")): AllocationDetail =
    allocationDetails(mainType, chargeType, localToDateOpt).copy(mainTransaction = Some("5070"))

  "AllocationDetail" when {

    "calling .getPaymentAllocationKeyInPaymentAllocations" should {

      "return a valid message" when {

        "provided with all subcharge types for POA1" in {
          allocationDetails(Some("SA Payment on Account 1"), Some("NIC4"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa1.nic4"
          allocationDetails(Some("SA Payment on Account 1"), Some("ITSA"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa1.incomeTax"
        }

        "provided with all subcharge types for POA2" in {
          allocationDetails(Some("SA Payment on Account 2"), Some("NIC4"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa2.nic4"
          allocationDetails(Some("SA Payment on Account 2"), Some("ITSA"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.poa2.incomeTax"
        }

        "provided with all subcharge types for a balancing charge" in {
          allocationDetails(Some("SA Balancing Charge"), Some("ITSA"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.incomeTax"
          allocationDetails(Some("SA Balancing Charge"), Some("NIC4"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic4"
          allocationDetails(Some("SA Balancing Charge"), Some("Voluntary NIC2"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.vcnic2"
          allocationDetails(Some("SA Balancing Charge"), Some("NIC2"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic2"
          allocationDetails(Some("SA Balancing Charge"), Some(SL), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.sl"
          allocationDetails(Some("SA Balancing Charge"), Some(CGT), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.cgt"

        }

        "provided with all subcharge types for an over-repayment charge (main transaction 5070)" in {
          overRepaymentAllocation(Some("ITSA England & NI")).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.incomeTax"
          overRepaymentAllocation(Some("NIC4-GB")).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.nic4"
          overRepaymentAllocation(Some("NIC2-GB")).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.nic2"
          overRepaymentAllocation(Some("Voluntary NIC2-GB")).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.vcnic2"
          overRepaymentAllocation(Some(SL)).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.sl"
          overRepaymentAllocation(Some(CGT)).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.cgt"
        }


        "provided with an over-repayment charge, whatever its mainType" in {
          overRepaymentAllocation(Some("ITSA England & NI"), mainType = Some("Any other text")).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.incomeTax"
          overRepaymentAllocation(Some("ITSA England & NI"), mainType = None).getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.overRepayment.incomeTax"
        }

        "provided with a main transaction other than 5070, using the mainType as before" in {
          allocationDetails(Some("SA Balancing Charge"), Some("NIC4"), localToDateOpt).copy(mainTransaction = Some("4910"))
            .getPaymentAllocationKeyInPaymentAllocations shouldBe "paymentAllocation.paymentAllocations.bcd.nic4"
        }

      }

      "return an empty message" when {
        "mainType and/or chargeType is None" in {
          allocationDetails(None, Some("NIC4"), localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
          allocationDetails(Some("SA Payment on Account 1"), None, localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
          allocationDetails(None, None, localToDateOpt).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
        }

        "an over-repayment charge has an unsupported or missing chargeType" in {
          overRepaymentAllocation(Some("Unknown charge type")).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
          overRepaymentAllocation(None).getPaymentAllocationKeyInPaymentAllocations shouldBe ""
        }
      }


    }

    "reading the payment allocation JSON sent by income-tax-financial-details" should {

      val backendAllocationJson: JsObject = Json.obj(
        "transactionId" -> "id",
        "from" -> "2019-01-04",
        "to" -> "2019-01-04",
        "chargeType" -> "ITSA England & NI",
        "mainType" -> "ITSA Over-repayment",
        "amount" -> 10000.0,
        "clearedAmount" -> 5000.0,
        "chargeReference" -> "chargeReference1"
      )

      "read mainTransaction when the backend sends it" in {
        (backendAllocationJson ++ Json.obj("mainTransaction" -> "5070")).as[AllocationDetail].mainTransaction shouldBe Some("5070")
      }

      "default mainTransaction to None when the backend does not send it" in {
        backendAllocationJson.as[AllocationDetail].mainTransaction shouldBe None
      }
    }

    "calling .getTaxYear" should {

      "determine the allocation tax year by the period end date in the model" in {
        def allocationDetailWithDateTo(taxPeriodEndDate: String): AllocationDetail = {
          AllocationDetail(Some("id"), Some(LocalDate.parse("2018-08-04")), to = Some(LocalDate.parse(taxPeriodEndDate)),
            None, Some("ITSA"), Some("SA Balancing Charge"), Some(10000.0), Some(5000.0), Some("chargeReference1"))
        }

        allocationDetailWithDateTo("2018-03-06").getTaxYear shouldBe 2018
        allocationDetailWithDateTo("2018-04-05").getTaxYear shouldBe 2018
        allocationDetailWithDateTo("2018-04-06").getTaxYear shouldBe 2019
        allocationDetailWithDateTo("2018-04-07").getTaxYear shouldBe 2019
        allocationDetailWithDateTo("2018-06-01").getTaxYear shouldBe 2019
      }

      "determine the allocation tax year by the fallback tax year in the model" in {
        AllocationDetail(Some("id"), Some(LocalDate.parse("2018-08-04")), to = None,
          Some(2022), Some("ITSA"), Some("SA Balancing Charge"), Some(10000.0), Some(5000.0), Some("chargeReference1")).getTaxYear shouldBe 2022
      }

    }

    "calling .getTaxYearOpt" should {
      "return the fallback tax year when periodEndDate is None" in {
        AllocationDetail(Some("id"), Some(LocalDate.parse("2018-08-04")), to = None,
          Some(2022), Some("ITSA"), Some("SA Balancing Charge"), Some(10000.0), Some(5000.0), Some("chargeReference1")).getTaxYearOpt shouldBe Some(2022)
      }
    }

    "calling .determineTaxYearFromPeriodEnd" should {
      "throw Exception" when {
        "periodEndDate is None" in {
          intercept[Exception] {
            allocationDetails(Some("SA Payment on Account 1"), Some("NIC4"), None).getTaxYear
          }
        }
      }
    }

    "calling .transactionIdActual" should {
      "throw MissingFieldException" when {
        "value is missing" in {
          intercept[MissingFieldException] {
            allocationDetails(Some("SA Payment on Account 1"), Some("NIC4"), None).copy(transactionId = None).transactionIdActual.equalsIgnoreCase("id")
          }
        }
      }
      "return value" when {
        "value is present" in{
          allocationDetails(Some("SA Payment on Account 1"), Some("NIC4"), None).transactionIdActual shouldBe "id"
        }
      }
    }

    "calling .amountActual" should {
      "throw MissingFieldException" when {
        "value is missing" in {
          intercept[MissingFieldException] {
            allocationDetails(Some("SA Payment on Account 1"), Some("NIC4"), None).copy(amount = None).amountActual shouldBe BigDecimal(10000.0)
          }
        }
      }
      "return value" when {
        "value is present" in{
          allocationDetails(Some("SA Payment on Account 1"), Some("NIC4"), None).amountActual shouldBe BigDecimal(10000.0)
        }
      }
    }
  }
}