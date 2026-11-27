/*
 * Copyright 2025 HM Revenue & Customs
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

package views.partials.yourSelfAssessmentCharges

import auth.MtdItUser
import auth.actions.AuthActionsTestData.defaultMTDITUser
import controllers.routes as financialsRoutes
import exceptions.MissingFieldException
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.*
import testConstants.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testConstants.PaymentAllocationsTestConstants.*
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import views.html.partials.yourSelfAssessmentCharges.ChargeSummaryPaymentAllocationTable

import java.time.LocalDate

class ChargeSummaryPaymentAllocationTableSpec extends TestSupport {

  lazy val table: ChargeSummaryPaymentAllocationTable = app.injector.instanceOf[ChargeSummaryPaymentAllocationTable]

  private def allocation(to: Option[LocalDate] = Some(LocalDate.parse("2019-08-27")),
                         fallbackTaxYear: Option[Int] = None,
                         mainType: String = "SA Payment on Account 1",
                         transactionId: String = "1040000872",
                         amount: BigDecimal = 10.10,
                         clearingDate: LocalDate = LocalDate.parse("2019-05-27")): AllocationDetailWithClearingDate =
    AllocationDetailWithClearingDate(
      Some(AllocationDetail(Some(transactionId), Some(LocalDate.parse("2019-06-27")), to, fallbackTaxYear,
        Some("NIC4 Wales"), Some(mainType), Some(amount), Some(5.50), Some("chargeReference1"))),
      Some(clearingDate)
    )

  private def nonLpiViewModel(allocations: Seq[AllocationDetailWithClearingDate]): PaymentAllocationViewModel =
    PaymentAllocationViewModel(paymentAllocationChargesModel, allocations)

  private def page(viewModel: PaymentAllocationViewModel,
                   creditsRefundsFSEnabled: Boolean = true,
                   affinity: AffinityGroup = Individual): Document = {
    implicit val user: MtdItUser[_] = defaultMTDITUser(Some(affinity), businessAndPropertyAligned)
    Jsoup.parse(contentAsString(table(viewModel, creditsRefundsFSEnabled)))
  }

  private val creditModel = PaymentAllocationViewModel(paymentAllocationChargesModelWithCredit, Seq())
  private val zeroOutstandingModel = PaymentAllocationViewModel(
    FinancialDetailsWithDocumentDetailsModel(List(documentDetailWithCredit.copy(outstandingAmount = 0)), List(financialDetail)), Seq())

  "ChargeSummaryPaymentAllocationTable" when {

    "rendering the table structure" should {
      "render the section heading and a govuk table with caption and headers" in {
        val doc = page(creditModel)
        doc.select("h2#allocation-section").text() shouldBe messages("yourSelfAssessmentChargeSummary.allocation.heading")

        val govukTable = doc.select("table#payment-allocation-table.govuk-table")
        govukTable.size() shouldBe 1
        govukTable.select("caption").text() shouldBe messages("paymentAllocation.tableSection.heading")
        govukTable.select("thead th").eachText().toArray shouldBe Array(
          messages("paymentAllocation.tableHead.allocated-date"),
          messages("paymentAllocation.tableHead.description"),
          messages("paymentAllocation.tableHead.tax-year"),
          messages("paymentAllocation.tableHead.amount")
        )
        govukTable.select("thead th").last().hasClass("govuk-table__header--numeric") shouldBe true
      }
    }

    "payment is not an LPI payment" should {

      "render one row per allocation with date, link, tax year and numeric amount" in {
        val doc = page(paymentAllocationViewModel)
        val first = paymentAllocationViewModel.originalPaymentAllocationWithClearingDate.head.allocationDetailActual
        val taxYear = first.getTaxYear

        doc.select("td[id^=payment-allocation-]").size() shouldBe 2
        val row = doc.select("tbody tr").first()
        row.select("td").get(0).text() shouldBe "27 May 2019"
        row.select("td#payment-allocation-0 a.govuk-link").attr("href") shouldBe
          financialsRoutes.ChargeSummaryController.show(taxYear, first.transactionIdActual).url
        row.select("td#payment-allocation-0 a").text() shouldBe messages(first.getPaymentAllocationKeyInPaymentAllocations)
        row.select("td").get(2).select("span.govuk-visually-hidden").text() shouldBe
          messages("paymentAllocation.taxYearHidden", (taxYear - 1).toString, taxYear.toString)
        row.select("td").get(2).select("span[aria-hidden=true]").text() shouldBe
          messages("paymentAllocation.taxYear", (taxYear - 1).toString, taxYear.toString)
        row.select("td").get(3).hasClass("govuk-table__cell--numeric") shouldBe true
        row.select("td").get(3).text() shouldBe "£10.10"
      }

      "link to the agent charge summary when the user is an agent" in {
        val doc = page(paymentAllocationViewModel, affinity = Agent)
        val first = paymentAllocationViewModel.originalPaymentAllocationWithClearingDate.head.allocationDetailActual
        doc.select("td#payment-allocation-0 a.govuk-link").attr("href") shouldBe
          financialsRoutes.ChargeSummaryController.showAgent(first.getTaxYear, first.transactionIdActual).url
      }

      "render a paragraph and no link when the allocation is an On Account allocation" in {
        val doc = page(nonLpiViewModel(Seq(allocation(mainType = "On Account"))))
        doc.select("td#payment-allocation-0 a").size() shouldBe 0
        doc.select("td#payment-allocation-0 p.govuk-body").text() shouldBe
          messages("paymentAllocation.paymentAllocations.onAccount.table.description.poa")
      }

      "render an unlinked description and 'No data' tax year when the allocation has no tax year" in {
        val doc = page(nonLpiViewModel(Seq(allocation(to = None, fallbackTaxYear = None))))
        val row = doc.select("tbody tr").first()
        row.select("td#payment-allocation-0 a.govuk-link").size() shouldBe 0
        row.select("td#payment-allocation-0 a.govuk-body.no-wrap").size() shouldBe 1
        row.select("td").get(2).select("span.govuk-visually-hidden").size() shouldBe 0
        row.select("td").get(2).text() shouldBe messages("paymentAllocation.noData")
      }

      "use the fallback tax year when there is no period end date" in {
        val doc = page(nonLpiViewModel(Seq(allocation(to = None, fallbackTaxYear = Some(2021)))))
        doc.select("td#payment-allocation-0 a.govuk-link").attr("href") shouldBe
          financialsRoutes.ChargeSummaryController.show(2021, "1040000872").url
        doc.select("tbody tr").first().select("td").get(2).select("span[aria-hidden=true]").text() shouldBe
          messages("paymentAllocation.taxYear", "2020", "2021")
      }

      "render a mixture of linked, on account and no tax year rows in order with sequential ids" in {
        val doc = page(nonLpiViewModel(Seq(
          allocation(),
          allocation(mainType = "On Account"),
          allocation(to = None)
        )))
        doc.select("td[id^=payment-allocation-]").size() shouldBe 3
        doc.select("td#payment-allocation-0 a.govuk-link").size() shouldBe 1
        doc.select("td#payment-allocation-1 p.govuk-body").size() shouldBe 1
        doc.select("td#payment-allocation-2 a.govuk-body.no-wrap").size() shouldBe 1
      }

      "render no allocation rows when there are no allocations" in {
        val doc = page(nonLpiViewModel(Seq()))
        doc.select("td[id^=payment-allocation-]").size() shouldBe 0
      }

      "throw when an allocation is missing its clearing date" in {
        val noClearingDate = allocation().copy(clearingDate = None)
        a[MissingFieldException] shouldBe thrownBy(page(nonLpiViewModel(Seq(noClearingDate))))
      }
    }

    "payment is an LPI payment" should {

      "render a single LPI row with 'No data' date, link, tax year and numeric amount" in {
        val doc = page(paymentAllocationViewModelLpi)
        val lpi = paymentAllocationViewModelLpi.latePaymentInterestPaymentAllocationDetails.get
        val taxYear = lpi.documentDetail.taxYear

        doc.select("td[id^=payment-allocation-]").size() shouldBe 1
        val row = doc.select("tbody tr").first()
        row.select("td").get(0).text() shouldBe messages("paymentAllocation.noData")
        row.select("td#payment-allocation-0 a.govuk-link").attr("href") shouldBe
          financialsRoutes.ChargeSummaryController.show(taxYear, lpi.documentDetail.transactionId, true).url
        row.select("td#payment-allocation-0 a").text() shouldBe
          messages(s"paymentAllocation.paymentAllocations.${lpi.documentDetail.getDocumentChargeTypeKey}")
        row.select("td").get(2).select("span.govuk-visually-hidden").text() shouldBe
          messages("paymentAllocation.taxYearHidden", (taxYear - 1).toString, taxYear.toString)
        row.select("td").get(2).select("span[aria-hidden=true]").text() shouldBe
          messages("paymentAllocation.taxYear", (taxYear - 1).toString, taxYear.toString)
        row.select("td").get(3).hasClass("govuk-table__cell--numeric") shouldBe true
        row.select("td").get(3).text() shouldBe "£300.00"
      }

      "link to the agent LPI charge summary when the user is an agent" in {
        val doc = page(paymentAllocationViewModelLpi, affinity = Agent)
        val lpi = paymentAllocationViewModelLpi.latePaymentInterestPaymentAllocationDetails.get
        doc.select("td#payment-allocation-0 a.govuk-link").attr("href") shouldBe
          financialsRoutes.ChargeSummaryController.showAgent(lpi.documentDetail.taxYear, lpi.documentDetail.transactionId, true).url
      }

      "not render the non-LPI allocations" in {
        val viewModel = paymentAllocationViewModelLpi.copy(
          originalPaymentAllocationWithClearingDate = paymentAllocationViewModel.originalPaymentAllocationWithClearingDate)
        page(viewModel).select("td[id^=payment-allocation-]").size() shouldBe 1
      }

      "render no LPI row when there are no LPI allocation details" in {
        val viewModel = paymentAllocationViewModelLpi.copy(latePaymentInterestPaymentAllocationDetails = None)
        page(viewModel).select("td[id^=payment-allocation-]").size() shouldBe 0
      }
    }

    "there is an outstanding credit amount" should {

      "render the money in account row with a link and numeric amount" in {
        val doc = page(creditModel)
        doc.select("td#money-on-account").text() shouldBe "31 Jan 2021"
        doc.select("a#money-on-account-link").text() shouldBe messages("paymentAllocation.moneyOnAccount")
        doc.select("a#money-on-account-link").attr("href") shouldBe financialsRoutes.MoneyInYourAccountController.show().url
        val row = doc.select("td#money-on-account").first().parent()
        row.select("td").get(2).text() shouldBe messages("paymentAllocation.not-applicable")
        row.select("td").get(2).hasClass("no-wrap") shouldBe true
        row.select("td").get(3).hasClass("govuk-table__cell--numeric") shouldBe true
        row.select("td").get(3).text() shouldBe "£200.00"
      }

      "link to the agent money in your account page when the user is an agent" in {
        page(creditModel, affinity = Agent).select("a#money-on-account-link").attr("href") shouldBe
          financialsRoutes.MoneyInYourAccountController.showAgent().url
      }

      "render plain text instead of a link when the credits and refunds feature switch is disabled" in {
        val doc = page(creditModel, creditsRefundsFSEnabled = false)
        doc.select("a#money-on-account-link").size() shouldBe 0
        doc.select("td#money-on-account").first().parent().select("td").get(1).text() shouldBe
          messages("paymentAllocation.moneyOnAccount")
      }

      "render the credit row after the allocation rows" in {
        val rows = page(paymentAllocationViewModel).select("tbody tr")
        rows.size() shouldBe 3
        rows.last().select("td#money-on-account").size() shouldBe 1
      }

      "render the credit row after the LPI row" in {
        val rows = page(paymentAllocationViewModelLpi).select("tbody tr")
        rows.size() shouldBe 2
        rows.last().select("td#money-on-account").size() shouldBe 1
      }

      "throw when the due date is missing" in {
        val noDueDate = creditModel.copy(paymentAllocationChargeModel = FinancialDetailsWithDocumentDetailsModel(
          List(documentDetailWithCredit), List(financialDetail.copy(items = financialDetail.items.map(_.map(_.copy(dueDate = None)))))))
        a[MissingFieldException] shouldBe thrownBy(page(noDueDate))
      }
    }

    "there is no outstanding credit amount" should {

      "not render the credit row when the outstanding amount is zero" in {
        val doc = page(zeroOutstandingModel)
        doc.select("td#money-on-account").size() shouldBe 0
        doc.select("tbody tr").size() shouldBe 0
      }

      "not render the credit row when there is no document detail" in {
        val noDocs = creditModel.copy(paymentAllocationChargeModel = FinancialDetailsWithDocumentDetailsModel(List(), List(financialDetail)))
        val doc = page(noDocs)
        doc.select("td#money-on-account").size() shouldBe 0
        doc.select("tbody tr").size() shouldBe 0
      }

      "still render only the allocation rows when the outstanding amount is zero" in {
        val viewModel = paymentAllocationViewModel.copy(paymentAllocationChargeModel =
          FinancialDetailsWithDocumentDetailsModel(List(documentDetail.copy(outstandingAmount = 0)), List(financialDetail)))
        val doc = page(viewModel)
        doc.select("td[id^=payment-allocation-]").size() shouldBe 2
        doc.select("td#money-on-account").size() shouldBe 0
      }
    }
  }
}
