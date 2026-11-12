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

package financials.views

import common.config.featureswitch.FeatureSwitching
import common.exceptions.MissingFieldException
import common.models.incomeSourceDetails.TaxYear
import common.testUtils.ViewSpec
import financials.controllers.routes as financialsRoutes
import financials.enums.*
import financials.enums.ChargeType.*
import financials.models.*
import financials.models.chargeHistory.{AdjustmentHistoryModel, AdjustmentModel, ChargeHistoryModel}
import financials.models.chargeSummary.{ChargeSummaryViewModel, PaymentHistoryAllocation, PaymentHistoryAllocations}
import financials.testConstants.ChargeConstants
import financials.testConstants.FinancialDetailsTestConstants.*
import financials.views.html.ChargeSummaryView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.Assertion
import play.api.mvc.AnyContentAsEmpty
import play.twirl.api.Html
import play.api.test.FakeRequest
import play.api.test.Helpers.GET

import java.time.{LocalDate, LocalDateTime, LocalTime}

class ChargeSummaryViewSpec extends ViewSpec with FeatureSwitching with ChargeConstants {

  lazy val chargeSummary: ChargeSummaryView = app.injector.instanceOf[ChargeSummaryView]
  val whatYouOweAgentUrl: String = financialsRoutes.WhatYouOweController.showAgent().url

  import Messages.*

  val defaultAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1400, Some(LocalDate.of(2018,3,29)), AdjustmentReversalReason), List())

  class TestSetup(chargeItem: ChargeItem = chargeItemModel(),
                  dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                  paymentBreakdown: List[FinancialDetail] = List(),
                  paymentAllocations: List[PaymentHistoryAllocations] = List(),
                  reviewAndReconcileCredit: Option[ChargeItem] = None,
                  payments: FinancialDetailsModel = payments,
                  chargeHistoryEnabled: Boolean = true,
                  latePaymentInterestCharge: Boolean = false,
                  isAgent: Boolean = false,
                  adjustmentHistory: AdjustmentHistoryModel = defaultAdjustmentHistory,
                  poaExtraChargeLink: Option[String] = None,
                  whatYouOweUrl: String = financialsRoutes.WhatYouOweController.show().url,
                  isStoodOverEnabled: Boolean = false) {

    val viewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
      currentDate = dateService.getCurrentDate,
      chargeItem = chargeItem.copy(dueDate = dueDate),
      backUrl = "testBackURL",
      gatewayPage = None,
      paymentBreakdown = paymentBreakdown,
      paymentAllocations = paymentAllocations,
      reviewAndReconcileCredit = reviewAndReconcileCredit,
      payments = payments,
      chargeHistoryEnabled = chargeHistoryEnabled,
      latePaymentInterestCharge = latePaymentInterestCharge,
      penaltiesEnabled = true,
      isAgent = isAgent,
      poaOneChargeUrl = "testUrl1",
      poaTwoChargeUrl = "testUrl2",
      adjustmentHistory = adjustmentHistory,
      poaExtraChargeLink = poaExtraChargeLink,
      LSPUrl = "testLSPUrl",
      LPPUrl = "testLPPUrl",
      taxYearSummaryUrl = (taxYear: Int) => appConfig.taxYearSummaryUrl(isAgent, taxYear)
    )

    val view: Html = chargeSummary(viewModel, None, whatYouOweUrl, isStoodOverEnabled)
    val document: Document = Jsoup.parse(view.toString())

    def verifyPaymentBreakdownRowNumeric(rowNumber: Int, expectedKeyText: String, expectedValueText: String): Assertion = {
      val paymentBreakdownRow = document.select(s".govuk-summary-list:nth-of-type(2) .govuk-summary-list__row:nth-of-type($rowNumber)")
      paymentBreakdownRow.select(".govuk-summary-list__key").text() shouldBe expectedKeyText
      paymentBreakdownRow.select(".govuk-summary-list__value--numeric").text() shouldBe expectedValueText
    }

    def verifyPaymentHistoryContent(rows: String*): Assertion = {
      document.select(Selectors.table).text() shouldBe
        s"""
           |Date Description Amount
           |${rows.mkString("\n")}
           |""".stripMargin.trim.linesIterator.mkString(" ")

    }

  }

  def reviewAndReconcileCreditChargeItem(transactionType: TransactionType): Option[ChargeItem] =
    Some(ChargeItem(
      transactionId = "some-id",
      taxYear = TaxYear(2019, 2020),
      transactionType = transactionType,
      codedOutStatus = None,
      documentDate = LocalDate.of(2018, 8, 6),
      dueDate = Some(LocalDate.of(2018, 8, 6)),
      originalAmount = 1000,
      outstandingAmount = 0,
      interestOutstandingAmount = None,
      accruingInterestAmount = None,
      latePaymentInterestAmount = None,
      interestFromDate = None,
      interestEndDate = None,
      interestRate = None,
      lpiWithDunningLock = None,
      amountCodedOut = None,
      dunningLock = false,
      poaRelevantAmount = None,
      chargeReference = Some("chargeRef"),
      chargeClassification = None
    ))

  def paymentsForCharge(mainType: String, chargeType: String, date: String, amount: BigDecimal, clearingSAPDocument: Option[String], clearingId: Option[String], taxYear: Option[String] = None): PaymentHistoryAllocations =
    PaymentHistoryAllocations(
      allocations = List(PaymentHistoryAllocation(
        dueDate = Some(LocalDate.parse(date)),
        amount = Some(amount),
        clearingSAPDocument = clearingSAPDocument,
        clearingId = clearingId,
        taxYear = taxYear)),
      chargeMainType = Some(mainType), chargeType = Some(chargeType))

  def codedOutEmptyAdjustmentHistory(originalAmount: BigDecimal): AdjustmentHistoryModel =
    AdjustmentHistoryModel(AdjustmentModel(originalAmount, Some(LocalDate.of(2018, 3, 29)), AdjustmentReversalReason), List())

  def codedOutAdjustmentHistory: AdjustmentHistoryModel =
    AdjustmentHistoryModel(AdjustmentModel(2500.00, Some(LocalDate.of(2018,3,29)), AdjustmentReversalReason), List(AdjustmentModel(2000.00, Some(LocalDate.of(2019,3,30)), AmendedReturnReversalReason)))

  object Messages {
    val typePOA1 = "SA Payment on Account 1"
    val typePOA2 = "SA Payment on Account 2"
    val typeBalCharge = "SA Balancing Charge"
    val taxYearHeading: String = messages("taxYears.table.taxYear.heading")
    val balancingCharge: String = messages("chargeSummary.balancingCharge.text")
    val paymentBreakdownNic2: String = messages("chargeSummary.paymentBreakdown.nic2")
    val codingOutBCDMessage2017To2018: String = messages("chargeSummary.codingOutBCDMessage", 2017, 2018)
    val codingOutBCDMessage2017To2018WithStringMessagesArgument: String = messages("yourSelfAssessmentChargeSummary.codingOutBCDMessage", "2017", "2018", "2019", "2020")
    val chargeSummaryCodingOutHeading2017To2018: String = s"${messages("chargeSummary.codingOut.text")}"
    val chargeSummaryPoa1CodedOutHeading: String = messages("chargeSummary.poa1CodedOut.text")
    val chargeSummaryPoa2CodedOutHeading: String = messages("chargeSummary.poa2CodedOut.text")
    val insetPara: String = s"${messages("chargeSummary.codingOutInset-1")} ${messages("chargeSummary.codingOutInset-2")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.codingOutInset-3")}"
    val paymentBreakdownInterestLocksCharging: String = messages("chargeSummary.paymentBreakdown.interestLocks.charging")

    val poaTextParagraph = messages("yourSelfAssessmentChargeSummary.whatIsPaymentOnAccount.p1")
    val poaTextBullet1 = messages("chargeSummary.paymentsOnAccount.bullet1")
    val poaTextBullet2 = messages("chargeSummary.paymentsOnAccount.bullet2")
    val poaTextP2 = messages("chargeSummary.paymentsOnAccount.p2")

    val adjustmentText = messages("chargeSummary.definition.hmrcadjustment")

    val lpiPoa1TextSentence = messages("chargeSummary.lpi.paymentsOnAccount.poa1")
    val lpiPoa2TextSentence = messages("chargeSummary.lpi.paymentsOnAccount.poa2")
    val lpiPoaTextParagraph = messages("chargeSummary.lpi.paymentsOnAccount.textOne") + " " + messages("chargeSummary.lpi.paymentsOnAccount.linkText") + " " + messages("chargeSummary.lpi.paymentsOnAccount.textTwo")

    def poa1ReconciliationInterestP1(startYear: String, endYear: String) = messages("yourSelfAssessmentChargeSummary.poa1ExtraAmountInterest.p1", startYear, endYear)
    val poa1ReconciliationInterestP2 = messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p1")
    val poa1ReconciliationInterestP3 = messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p2") + " " +  messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p2LinkText") + " " + messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p2AfterLink")

    def poa2ReconciliationInterestP1(startYear: String, endYear: String) = messages("yourSelfAssessmentChargeSummary.poa2ExtraAmountInterest.p1", startYear, endYear)
    val poa2ReconciliationInterestP2 = messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p1")
    val poa2ReconciliationInterestP3 = messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p2") + " " +  messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p2LinkText") + " " + messages("yourSelfAssessmentChargeSummary.extraAmountInterest.p2AfterLink")

    val poaExtraChargeText1: String = messages("chargeSummary.extraCharge.text1")
    val poaExtraChargeTextLink: String = messages("chargeSummary.extraCharge.linkText")
    val poaExtraChargeText2: String = messages("chargeSummary.extraCharge.text2")

    val lspContent1: String = messages("chargeSummary.lateSubmissionPenalty.penaltyPoints")
    val lspContent2: String = messages("chargeSummary.lateSubmissionPenalty.fourPoints")
    val lspContent3: String = messages("chargeSummary.lateSubmissionPenalty.avoidInFuture")
    val lspContent4: String = messages("chargeSummary.lateSubmissionPenalty.penaltyLinkText1") + " " + messages("chargeSummary.lateSubmissionPenalty.penaltyLinkText") + " " +
      messages("chargeSummary.lateSubmissionPenalty.penaltyLinkText2")

    val lpp1Content1: String = messages("chargeSummary.firstLatePaymentPenalty.p")
    val lpp1Content2: String = messages("chargeSummary.firstLatePaymentPenalty.link")

    val bcdTextParagraph = messages("chargeSummary.definition.balancingcharge.p1")
    val bcdTextBullets = messages("chargeSummary.definition.balancingcharge.bullet1")
    val bcdTextP2 = messages("chargeSummary.definition.balancingcharge.bullet2")

    val lpiBcdTextSentence = messages("chargeSummary.lpi.balancingCharge.p1")
    val lpiBcdTextParagraph = messages("chargeSummary.lpi.balancingCharge.textOne") + " " + messages("chargeSummary.lpi.balancingCharge.linkText") + " " + messages("chargeSummary.lpi.balancingCharge.textTwo")
    val lpiBcdTextP3 = messages("chargeSummary.lpi.balancingCharge.p3") + " " + messages("chargeSummary.lpi.balancingCharge.p3LinkText")


    def poaHeading(year: Int, number: Int) = s"${year - 1} to $year tax year ${getFirstOrSecond(number)} payment on account"
    def poa1Caption(year: Int) = s"This section is ${year - 1} to $year tax year"

    def poa2Heading = s"Second payment on account"

    def getFirstOrSecond(number: Int): String = {
      require(number > 0, "Number must be greater than zero")
      number match {
        case 1 => "First"
        case 2 => "Second"
        case _=> throw new Error(s"Number must be 1 or 2 but got: $number")
      }
    }

    def poa1InterestHeading = s"Late payment interest on first payment on account"

    def poa2InterestHeading = s"Late payment interest on second payment on account"

    def poa1ReconcileHeading = s"First payment on account: extra amount from your tax return"

    def poa2ReconcileHeading = s"Second payment on account: extra amount from your tax return"

    def poa1ReconcileInterestHeading = s"Interest for first payment on account: extra amount"

    def poa2ReconcileInterestHeading = s"Late payment interest for second payment on account: extra amount"

    val lateSubmissionPenaltyHeading: String = "Late submission penalty"

    val firstLatePaymentPenalty: String = "First late payment penalty"

    def poa1ReconciliationCreditHeading = s"First payment on account: credit from your tax return"

    def poa2ReconciliationCreditHeading = s"Second payment on account: credit from your tax return"

    def balancingChargeHeading = s"$balancingCharge"

    def balancingChargeInterestHeading = s"${messages("chargeSummary.lpi.balancingCharge.text")}"

    def class2NicHeading = s"$paymentBreakdownNic2"

    def cancelledSaPayeHeading = s"${messages("chargeSummary.cancelledPayeSelfAssessment.text")}"

    val dueDate: String = messages("chargeSummary.dueDate")
    val interestPeriod: String = messages("chargeSummary.lpi.interestPeriod")
    val fullPaymentAmount: String = messages("chargeSummary.paymentAmount")
    val paymentAmount: String = messages("chargeSummary.paymentAmountCodingOut")
    val amount: String = messages("chargeSummary.chargeHistory.amount")
    val remainingToPay: String = messages("chargeSummary.remainingDue")
    val paymentBreakdownHeading: String = messages("chargeSummary.paymentBreakdown.heading")
    val chargeHistoryHeadingGeneric: String = messages("chargeSummary.chargeHistory.heading")
    val historyRowPOA1Created: String = s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.paymentOnAccount1.text")} £1,400.00"
    val codingOutHeader: String = "2017 to 2018 tax year PAYE self assessment"
    val paymentprocessingbullet1: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")} ${messages("pagehelp.opensInNewTabText")}"
    val paymentprocessingbullet1Agent: String = s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2-agent")} ${messages("pagehelp.opensInNewTabText")}"

    def paymentOnAccountCreated(number: Int) = messages(s"chargeSummary.chargeHistory.created.paymentOnAccount$number.text")

    def firstLatePaymentOnAccountInterestCreated = s"Late payment interest for first payment on account created"

    def secondLatePaymentOnAccountInterestCreated = s"Late payment interest for second payment on account created"


    val lpiPoaOne: String = messages("chargeSummary.lpi.chargeHistory.created.reviewAndReconcilePoa1.text")
    val lpiPoaTwo: String = messages("chargeSummary.lpi.chargeHistory.created.reviewAndReconcilePoa2.text")

    val balancingChargeCreated: String = messages("chargeSummary.chargeHistory.created.balancingCharge.text")
    val balancingChargeInterestCreated: String = messages("chargeSummary.lpi.chargeHistory.created.balancingCharge.text")

    def paymentOnAccountAmended(number: Int) = s"Payment on account $number of 2 reduced due to amended return"

    def firstPoaAdjusted = "You updated your first payment on account"
    def secondPoaAdjusted = "You updated your second payment on account"

    val balancingChargeAmended: String = messages("chargeSummary.chargeHistory.amend.balancingCharge.text")

    def firstPaymentOnAccountRequest = s"First payment on account reduced by taxpayer request"
    def secondPaymentOnAccountRequest = s"Second payment on account reduced by taxpayer request"


    def class2NicTaxYear(year: Int) = messages("chargeSummary.nic2TaxYear", (year - 1).toString, year.toString)

    val class2NicChargeCreated: String = messages("chargeSummary.chargeHistory.created.class2Nic.text")
    val cancelledSaPayeCreated: String = messages("chargeSummary.chargeHistory.created.cancelledPayeSelfAssessment.text")

    def payeTaxCodeText(year: Int) = s"${messages("chargeSummary.check-paye-tax-code-1")} ${messages("chargeSummary.check-paye-tax-code-2")} ${messages("chargeSummary.check-paye-tax-code-3", year - 1, year)}"

    val cancelledPayeTaxCodeInsetText = s"${messages("chargeSummary.cancelledPayeInset-1")} ${messages("chargeSummary.cancelledPayeInset-2")} ${messages("pagehelp.opensInNewTabText")} ${messages("chargeSummary.cancelledPayeInset-3")}"
    val cancelledPayeTaxCodeInsetLink = "https://www.gov.uk/pay-self-assessment-tax-bill/through-your-tax-code"

    def remainingTaxYouOwe(year: Int) = messages("chargeSummary.codingOutBCDMessage", year - 1, year)

    val balancingChargeRequest: String = messages("chargeSummary.chargeHistory.request.balancingCharge.text")
    val dunningLockBannerHeader: String = messages("chargeSummary.dunning.locks.banner.title")
    val dunningLockBannerLink: String = s"${messages("chargeSummary.dunning.locks.banner.linkText")} ${messages("pagehelp.opensInNewTabText")}."
    val interestLinkFirstWord: String = messages("chargeSummary.whatYouOwe.textOne")
    val interestLinkFirstWordAgent: String = messages("chargeSummary.whatYouOwe.textOne-agent")
    val interestLinkText: String = messages("chargeSummary.whatYouOwe.linkText")
    val interestSALinkText: String = messages("chargeSummary.selfAssessmentCharges.linkText")
    val interestLinkTextAgent: String = messages("chargeSummary.whatYouOwe.linkText-agent")
    val interestSALinkTextAgent: String = messages("chargeSummary.selfAssessmentCharges.linkText-agent")
    val interestLinkFullText: String = messages("chargeSummary.interestLocks.text")
    val interestLinkFullTextAgent: String = messages("chargeSummary.interestLocks.text-agent")
    val cancelledPAYESelfAssessment: String = messages("whatYouOwe.cancelled-paye-sa.heading")
    val incomeTax: String = messages("chargeSummary.check-paye-tax-code-1")

    def dunningLockBannerText(formattedAmount: String, date: String) =
      s"$dunningLockBannerLink ${messages("chargeSummary.dunning.locks.banner.note", s"$formattedAmount", s"$date")}"

    val itsaOverRepaymentHeading = "Refund to repay after a change to your tax return"
    val itsaOverRepaymentParagraph1 = "We’ve checked the recent change to your Self Assessment tax return and found that the refund you previously claimed is higher than the amount you’re entitled to."
    val itsaOverRepaymentParagraph2 = "This means you’ve been overpaid by £1,400.00 for the 2017 to 2018 tax year."
    val itsaOverRepaymentDescriptionText = "Extra amount created because you corrected your tax return"
  }

  val amendedChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", fixedDate, "", 1500,
    LocalDateTime.of(LocalDate.of(2018, 7, 6), LocalTime.of(9, 30, 45)), "amended return", Some("001"))
  val amendedAdjustmentHistory: AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, CreateReversalReason),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), AdjustmentReversalReason))
  )
  val adjustmentHistoryWithBalancingCharge: AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, CreateReversalReason),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), AmendedReturnReversalReason))
  )
  val customerRequestChargeHistoryModel: ChargeHistoryModel = ChargeHistoryModel("", "", fixedDate, "", 1500, LocalDateTime.of(LocalDate.of(2018, 7, 6), LocalTime.of(9, 30, 45)), "Customer Request", Some("002"))
  val customerRequestAdjustmentHistory : AdjustmentHistoryModel = AdjustmentHistoryModel(
    creationEvent = AdjustmentModel(1400, None, CreateReversalReason),
    adjustments = List(AdjustmentModel(1500, Some(LocalDate.of(2018, 7, 6)), CustomerRequestReason))
  )

  val paymentBreakdown: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, additionalSubItems = Seq(SubItem(
      amount = Some(500.0),
      dueDate = Some(LocalDate.parse("2018-09-08")),
      clearingDate = Some(LocalDate.parse("2018-09-07")),
      clearingSAPDocument = Some("123456789012"),
      paymentAmount = Some(500.0),
      paymentLot = None,
      paymentLotItem = None)
    )),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB),
    financialDetail(originalAmount = 3456.78, chargeType = VOLUNTARY_NIC2_NI),
    financialDetail(originalAmount = 5678.9, chargeType = NIC4_WALES),
    financialDetail(originalAmount = 9876.54, chargeType = CGT),
    financialDetail(originalAmount = 543.21, chargeType = SL)
  )

  val paymentBreakdownWithDunningLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 9876.54, chargeType = CGT, dunningLock = Some("Stand over order")),
    financialDetail(originalAmount = 543.21, chargeType = SL)
  )

  val paymentBreakdownWithInterestLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal")),
    financialDetail(originalAmount = 9876.54, chargeType = CGT, interestLock = Some("Manual RPI Signal"), accruedInterest = Some(35)),
    financialDetail(originalAmount = 543.21, chargeType = SL)
  )

  val paymentBreakdownWhenInterestAccrues: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal"))
  )

  val paymentBreakdownWithPreviouslyAccruedInterest: List[FinancialDetail] = List(
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal"), accruedInterest = Some(30))
  )

  val paymentBreakdownWithMixedLocks: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, dunningLock = Some("Stand over order"), interestLock = Some("Clerical Interest Signal"))
  )

  val paymentBreakdownWithOnlyAccruedInterest: List[FinancialDetail] = List(
    financialDetail(originalAmount = 123.45, chargeType = ITSA_ENGLAND_AND_NI, accruedInterest = Some(30)),
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB)
  )

  val paymentBreakdownWithOnlyInterestLock: List[FinancialDetail] = List(
    financialDetail(originalAmount = 2345.67, chargeType = NIC2_GB, interestLock = Some("Clerical Interest Signal"))
  )

  def subItemWithClearingSapDocument(clearingSAPDocument: String): SubItem = SubItem(dueDate = Some(LocalDate.parse("2017-08-07")), clearingSAPDocument = Some(clearingSAPDocument), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))

  val payments: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None, None),
    documentDetails = List(DocumentDetail(9999, "PAYID01", Some("Payment on Account"), Some("documentText"), -5000, -15000, LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, None, Some("lotItem"), Some("lot")),
      DocumentDetail(2025, "123456789", Some("Reconciliation Credit"), Some("documentText"), 1200, 5000, LocalDate.of(2025, 2, 15), None, None, None, None, None, None, None, None, None, None, None, None, None, None)),
    financialDetails = List(FinancialDetail("9999", transactionId = Some("PAYID01"), items = Some(Seq(
      subItemWithClearingSapDocument("123456789012"),
      subItemWithClearingSapDocument("223456789012"),
      subItemWithClearingSapDocument("323456789012"),
      subItemWithClearingSapDocument("423456789012"),
      subItemWithClearingSapDocument("523456789012"),
      subItemWithClearingSapDocument("623456789012"),
      subItemWithClearingSapDocument("723456789012"),
      subItemWithClearingSapDocument("823456789012"),
      subItemWithClearingSapDocument("923456789012"),
      subItemWithClearingSapDocument("023456789012")
    ))),
      FinancialDetail("2025", Some("Reconciliation Credit"), Some("4905"), Some("123456789"), None, Some("1234"), None, Some(3800.00), Some(5000.00), None, Some(3800.00), Some("NIC4-GB"), None, None))
  )

  val creditsWithBreakdownPages: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None, None),
    documentDetails = List(DocumentDetail(2026, "PAYID01", Some("POA 1 Reconciliation Credit"), Some("documentText"), -5000, -15000, LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, None, Some("lotItem"), Some("lot")),
      DocumentDetail(2025, "123456789", Some("Reconciliation Credit"), Some("documentText"), 1200, 5000, LocalDate.of(2025, 2, 15), None, None, None, None, None, None, None, None, None, None, None, None, None, None)),
    financialDetails = List(FinancialDetail("2026", transactionId = Some("PAYID01"), mainTransaction = Some("4912"), items = Some(Seq(
      subItemWithClearingSapDocument("123456789012"),
      subItemWithClearingSapDocument("223456789012"),
      subItemWithClearingSapDocument("323456789012"),
      subItemWithClearingSapDocument("423456789012"),
      subItemWithClearingSapDocument("523456789012"),
      subItemWithClearingSapDocument("623456789012"),
      subItemWithClearingSapDocument("723456789012"),
      subItemWithClearingSapDocument("823456789012"),
      subItemWithClearingSapDocument("923456789012"),
      subItemWithClearingSapDocument("023456789012")
    ))),
      FinancialDetail("2025", Some("Reconciliation Credit"), Some("4916"), Some("123456789"), None, Some("1234"), None, Some(3800.00), Some(5000.00), None, Some(3800.00), Some("NIC4-GB"), None, None))
  )

  val creditsWithoutBreakdownPages: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None, None),
    documentDetails = List(DocumentDetail(2026, "PAYID01", Some("POA 1 Reconciliation Credit"), Some("documentText"), -5000, -15000, LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, None, Some("lotItem"), Some("lot")),
      DocumentDetail(2025, "123456789", Some("Reconciliation Credit"), Some("documentText"), 1200, 5000, LocalDate.of(2025, 2, 15), None, None, None, None, None, None, None, None, None, None, None, None, None, None)),
    financialDetails = List(FinancialDetail("2026", transactionId = Some("PAYID01"), mainTransaction = Some("4915"), items = Some(Seq(
      subItemWithClearingSapDocument("123456789012"),
      subItemWithClearingSapDocument("223456789012"),
      subItemWithClearingSapDocument("323456789012"),
      subItemWithClearingSapDocument("423456789012"),
      subItemWithClearingSapDocument("523456789012"),
      subItemWithClearingSapDocument("623456789012"),
      subItemWithClearingSapDocument("723456789012"),
      subItemWithClearingSapDocument("823456789012"),
      subItemWithClearingSapDocument("923456789012"),
      subItemWithClearingSapDocument("023456789012")
    ))),
      FinancialDetail("2025", Some("Reconciliation Credit"), Some("4915"), Some("123456789"), None, Some("1234"), None, Some(3800.00), Some(5000.00), None, Some(3800.00), Some("NIC4-GB"), None, None))
  )

  def checkPaymentProcessingInfo(document: Document): Unit = {
    document.select("#payment-days-note").text() shouldBe messages("chargeSummary.payment-days-note")
    document.select("#payment-processing-bullets li:nth-child(1)").text() shouldBe
      s"${messages("chargeSummary.payments-bullet1-1")} ${messages("chargeSummary.payments-bullet1-2")}"
    document.select("#payment-processing-bullets li:nth-child(2)").text() shouldBe messages("chargeSummary.payments-bullet2")
  }

  val itsaEnquiryAmendmentHeading: String = "Extra amount to pay due to HMRC enquiry amendment"
  val itsaEnquiryAmendmentParagraph1: (Int, Int) => String = (taxYear1, taxYear2) => s"Following a compliance check, HMRC made a change to your tax " +
    s"return known as an ‘enquiry amendment’. This changed your tax calculation for ${taxYear1.toString} to " +
    s"${taxYear2.toString}, resulting in an extra amount to pay towards your tax bill."
  val warningText: String = "Warning Pay this charge to stop this interest from increasing daily."
  val enquiryAmendmentDescriptionText: String = "Extra amount created when HMRC amended your tax return"
  val firstPaymentOnAccountHeading: String = messages("chargeSummary.chargeHistory.heading")
  val createdYourFirstPaymentOnAccountText: String = messages("chargeSummary.chargeHistory.created.paymentOnAccount1.text")
  val createdFirstLatePaymentPenaltyStoodOverText: String = messages("chargeSummary.chargeHistory.created.firstLatePaymentPenaltyStoodOver.text")
  val formalStandoverChargeHistoryText: String = messages("chargeSummary.dunning.locks.formal.standover.history")
  val dunningLocksBannerTitle: String = messages("chargeSummary.dunning.locks.banner.title")
  val dunningLocksBannerHeading: String = s"${messages("chargeSummary.dunning.locks.informal.standover.banner.note1")} ${messages("chargeSummary.dunning.locks.informal.standover.banner.note2")}"
  val formalDunningLocksBannerHeading: String = s"${messages("chargeSummary.dunning.locks.formal.standover.banner.note1")} ${messages("chargeSummary.dunning.locks.formal.standover.banner.note2")}"
  val itsaEnquiryAmendmentCreditHeading: String = "Credit from HMRC enquiry amendment"
  val chargeAmountHeading: String = messages("yourSelfAssessmentChargeSummary.stoodOverFullCharge")
  val firstLatePaymentPenaltyText: String = messages("chargeSummary.chargeHistory.created.firstLatePaymentPenalty.text")
  val firstPaymentOnAccount1Text: String = messages("chargeSummary.chargeHistory.created.firstLatePaymentPenalty.text")
  val chargeSummaryNoDataText: String = messages("chargeSummary.noData")
  val chargeAmountOverDueChargeHeading: String = messages("yourSelfAssessmentChargeSummary.overDueCharge")
  val createdBalancingPaymentText: String = messages("chargeSummary.chargeHistory.created.balancingCharge.text")

  def partialInformalCollectableBannerHeading(stoodOverValue: String): String =
    s"${messages("chargeSummary.dunning.locks.partial.informal.collectable.standover.banner.note1", stoodOverValue)} ${messages("chargeSummary.dunning.locks.partial.informal.collectable.standover.banner.note2")}"

  def partialFormalCollectableBannerHeading(stoodOverValue: String): String =
    s"${messages("chargeSummary.dunning.locks.partial.formal.collectable.standover.banner.note1", stoodOverValue)} ${messages("chargeSummary.dunning.locks.partial.formal.collectable.standover.banner.note2")}"

  val partialInformalStoodOverBannerHeading: String =
    s"${messages("chargeSummary.dunning.locks.informal.standover.banner.note1")} ${messages("chargeSummary.dunning.locks.informal.standover.banner.note2")}"

  val partialFormalStoodOverBannerHeading: String =
    s"${messages("chargeSummary.dunning.locks.formal.standover.banner.note1")} ${messages("chargeSummary.dunning.locks.formal.standover.banner.note2")}"

  def subItemWithClearingSapDocumentv2(clearingSAPDocument: String): SubItem = SubItem(dueDate = Some(LocalDate.parse("2017-08-07")), clearingSAPDocument = Some(clearingSAPDocument), paymentLot = Some("lot"), paymentLotItem = Some("lotItem"))

  val defaultAdjustmentHistoryv2: AdjustmentHistoryModel = AdjustmentHistoryModel(AdjustmentModel(1400, Some(LocalDate.of(2018, 3, 29)), AdjustmentReversalReason), List())

  val paymentsv2: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 0.00, 3.00, None, None, None, None, None, None, None, None),
    documentDetails = List(DocumentDetail(9999, "PAYID01", Some("Payment on Account"), Some("documentText"), -5000, -15000, LocalDate.of(2018, 8, 6), None, None, None, None, None, None, None, None, Some("lotItem"), Some("lot")),
      DocumentDetail(2025, "123456789", Some("Reconciliation Credit"), Some("documentText"), 1200, 5000, LocalDate.of(2025, 2, 15), None, None, None, None, None, None, None, None, None, None, None, None, None, None)),
    financialDetails = List(FinancialDetail("9999", transactionId = Some("PAYID01"), items = Some(Seq(
      subItemWithClearingSapDocument("123456789012"),
      subItemWithClearingSapDocument("223456789012"),
      subItemWithClearingSapDocument("323456789012"),
      subItemWithClearingSapDocument("423456789012"),
      subItemWithClearingSapDocument("523456789012"),
      subItemWithClearingSapDocument("623456789012"),
      subItemWithClearingSapDocument("723456789012"),
      subItemWithClearingSapDocument("823456789012"),
      subItemWithClearingSapDocument("923456789012"),
      subItemWithClearingSapDocument("023456789012")
    ))),
      FinancialDetail("2025", Some("Reconciliation Credit"), Some("4905"), Some("123456789"), None, Some("1234"), None, Some(3800.00), Some(5000.00), None, Some(3800.00), Some("NIC4-GB"), None, None))
  )

  val fakeRequest = FakeRequest(GET, "/tax-years/:taxYear/charge")
  val fakeRequestWithIsPartialSuspendedChargeFalse = FakeRequest(GET, "/tax-years/:taxYear/charge?isPartialSuspendedCharge=false")
  val fakeRequestWithIsPartialSuspendedChargeTrue = FakeRequest(GET, "/tax-years/:taxYear/charge?isPartialSuspendedCharge=true")

  class TestSetupv2(chargeItem: ChargeItem = chargeItemModel(),
                  dueDate: Option[LocalDate] = Some(LocalDate.of(2019, 5, 15)),
                  paymentBreakdown: List[FinancialDetail] = List(),
                  paymentAllocations: List[PaymentHistoryAllocations] = List(),
                  reviewAndReconcileCredit: Option[ChargeItem] = None,
                  payments: FinancialDetailsModel = payments,
                  chargeHistoryEnabled: Boolean = true,
                  latePaymentInterestCharge: Boolean = false,
                  isAgent: Boolean = false,
                  adjustmentHistory: AdjustmentHistoryModel = defaultAdjustmentHistory,
                  poaExtraChargeLink: Option[String] = None,
                  whatYouOweUrl: String = financialsRoutes.WhatYouOweController.show().url,
                  taxYearSummaryUrl: Int => String = (taxYear: Int) => appConfig.taxYearSummaryUrl(false, 2018),
                  isStoodOverEnabled: Boolean = false,
                  mockFakeRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest) {

    val viewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
      currentDate = dateService.getCurrentDate,
      chargeItem = chargeItem.copy(dueDate = dueDate),
      backUrl = "testBackURL",
      gatewayPage = None,
      paymentBreakdown = paymentBreakdown,
      paymentAllocations = paymentAllocations,
      reviewAndReconcileCredit = reviewAndReconcileCredit,
      payments = payments,
      chargeHistoryEnabled = chargeHistoryEnabled,
      latePaymentInterestCharge = latePaymentInterestCharge,
      penaltiesEnabled = true,
      isAgent = isAgent,
      poaOneChargeUrl = "testUrl1",
      poaTwoChargeUrl = "testUrl2",
      adjustmentHistory = adjustmentHistory,
      poaExtraChargeLink = poaExtraChargeLink,
      LSPUrl = "testLSPUrl",
      LPPUrl = "testLPPUrl",
      taxYearSummaryUrl = taxYearSummaryUrl
    )


    val viewV2: Html = chargeSummary(viewModel, None, whatYouOweUrl, isStoodOverEnabled = isStoodOverEnabled)(mockFakeRequest, implicitly, implicitly)
    val document: Document = Jsoup.parse(viewV2.toString())
  }
  "user is an individual" when {

    "render the row for the charge" should {
      "charge is a Review and Reconcile credit for Payment on Account 1" in new TestSetup(
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaOneReconciliationCredit)
      ) {

        document.selectById("rar-charge-link").text() shouldBe "First payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe financialsRoutes.ChargeSummaryController.show(2020, "some-id").url

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(5)
          .text() shouldBe "£1,000.00"

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(3)
          .text() shouldBe "6 Aug 2018"
      }
      "charge is a Review and Reconcile credit for Payment on Account 2" in new TestSetup(
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaTwoReconciliationCredit)
      ) {
        document.selectById("rar-charge-link").text() shouldBe "Second payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe financialsRoutes.ChargeSummaryController.show(2020, "some-id").url

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(5)
          .text() shouldBe "£1,000.00"

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(3)
          .text() shouldBe "6 Aug 2018"
      }
    }

    "charge is a POA1" when {

      val basePoaOneDebit = chargeItemModel(transactionType = PoaOneDebit)

      "no late payment interest" should {

        "have the correct caption" in new TestSetup(
          chargeItem = basePoaOneDebit
        ) {
          document.select("h1").text() shouldBe "First payment on account"
        }

        "have content explaining the definition of a payment on account when charge is a POA1" in new TestSetup(
          chargeItem = basePoaOneDebit
        ) {
          document.getElementById("howIsInterestCalculated.p1").text() shouldBe poaTextParagraph
          document.getElementById("howIsInterestCalculated.list").child(0).text() shouldBe poaTextBullet1
          document.getElementById("howIsInterestCalculated.list").child(1).text() shouldBe poaTextBullet2
          document.getElementById("howInterestIsCalculated.p2").text() shouldBe poaTextP2
        }

        "display only the charge creation item when no history found for a payment on account 1 of 2" in new TestSetup(
          chargeItem = chargeItemModel().copy(outstandingAmount = 0)
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
          document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(1)
          document.select("tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
        }


        "display the correct message for an amended charge for a payment on account 1 of 2" in new TestSetup(
          chargeItem = chargeItemModel().copy(outstandingAmount = 0),
          adjustmentHistory = amendedAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(1)").text() shouldBe "6 Jul 2018"
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe firstPoaAdjusted
          document.select("tbody tr:nth-child(2) td:nth-child(3)").text() shouldBe "£1,500.00"
        }

        "display the correct message for a customer requested change for a payment on account 1 of 2" in new TestSetup(
          chargeItem = chargeItemModel().copy(outstandingAmount = 0),
          adjustmentHistory = customerRequestAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe firstPaymentOnAccountRequest
        }
      }

      "late payment interest" should {

        "have the correct heading for a late interest charge" in new TestSetup(
          chargeItem = basePoaOneDebit,
          latePaymentInterestCharge = true
        ) {
          document.select("h1").text() shouldBe poa1InterestHeading
        }

        "have content explaining the definition of a late payment interest charge on account when charge is a POA1" in new TestSetup(
          chargeItem = basePoaOneDebit,
          latePaymentInterestCharge = true
        ) {
          document.selectById("lpi-poa2").text() shouldBe lpiPoa1TextSentence
          document.selectById("lpi-poa5").text() shouldBe lpiPoaTextParagraph
          document.selectById("lpi-poa6").text() shouldBe "See guidance on the interest rates set by HMRC (opens in new tab)"
        }
      }

      "have the correct caption" in new TestSetup(
        chargeItem = basePoaOneDebit
      ) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      }

      "display only the charge creation item for a payment on account 1 of 2 late interest charge" in new TestSetup(
        basePoaOneDebit.copy(outstandingAmount = 0),
        latePaymentInterestCharge = true
      ) {
        document.select("tbody tr").size() shouldBe 1
        document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
        document.select("tbody tr td:nth-child(2)").text() shouldBe firstLatePaymentOnAccountInterestCreated
        document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
      }

      "charge is coded out" when {
        "coding out is accepted" should {
          "display the accepted coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(Accepted), originalAmount = 2500.00)
            val codedOutBCDItem = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted), originalAmount = 2500.00)

            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem, adjustmentHistory = codedOutEmptyAdjustmentHistory(2500.00)) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa1CodedOutHeading
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 2
              document.select("#payment-history-table tbody tr").size() shouldBe 1
              document.select("#payment-history-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.poa1CodedOut.text", "2019", "2020")} £2,500.00"
            }
            "Coding Out is Enabled for BCD" in new TestSetup(codedOutBCDItem, adjustmentHistory = codedOutAdjustmentHistory) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 2
              document.select("#payment-history-table tbody tr").size() shouldBe 2
              document.select("#payment-history-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.poa1CodedOut.text", "2019", "2020")} £2,500.00"
              document.select("#payment-history-table tbody tr").get(1).text() shouldBe s"30 Mar 2019 ${messages("chargeSummary.chargeHistory.amend.codingOut.text", "2019", "2020")} £2,000.00"
            }
          }
        }
        "coding out is fully collected" should {
          "display the fully collected coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaOneDebit, codedOutStatus = Some(FullyCollected), originalAmount = 2500.00)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem, adjustmentHistory = codedOutEmptyAdjustmentHistory(2500.00)) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa1CodedOutHeading
              document.getElementById("howIsInterestCalculated.p1").text() shouldBe "’Payments on account’ are two advance payments made towards your next tax bill."
              document.getElementById("howIsInterestCalculated.list").child(0).text() shouldBe "Income Tax"
              document.getElementById("howIsInterestCalculated.list").child(1).text() shouldBe "Class 4 National Insurance contributions (opens in new tab)"
              document.getElementById("howIsInterestCalculated.list").child(1).link.attr("href") shouldBe "https://www.gov.uk/self-employed-national-insurance-rates"
              document.getElementById("howInterestIsCalculated.p2").text() shouldBe "HMRC estimates the total amount based on your previous year’s tax bill. Each payment is half of that amount."
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 2
              document.select("#payment-history-table tbody tr").size() shouldBe 1
              document.select("#payment-history-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.poa1CodedOut.text", "2019", "2020")} £2,500.00"
            }
          }
        }
      }
    }

    "charge is a POA2" when {

      val basePoaTwoDebit = chargeItemModel(transactionType = PoaTwoDebit)

      "no late payment interest" should {
        "have the correct heading and caption for a POA 2" in new TestSetup(
          chargeItem = basePoaTwoDebit
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe poa2Heading
        }

        "have content explaining the definition of a payment on account when charge is a POA2" in new TestSetup(
          chargeItem = basePoaTwoDebit
        ) {
          document.getElementById("howIsInterestCalculated.p1").text() shouldBe poaTextParagraph
          document.getElementById("howIsInterestCalculated.list").child(0).text() shouldBe poaTextBullet1
          document.getElementById("howIsInterestCalculated.list").child(1).text() shouldBe poaTextBullet2
          document.getElementById("howInterestIsCalculated.p2").text() shouldBe poaTextP2
        }

        "display only the charge creation item when no history found for a payment on account 2 of 2" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = PoaTwoDebit, outstandingAmount = 0)
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(2)").text() shouldBe paymentOnAccountCreated(2)
        }

        "display the correct message for an amended charge for a payment on account 2 of 2" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = PoaTwoDebit, outstandingAmount = 0),
          adjustmentHistory = amendedAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe secondPoaAdjusted
        }

        "display the correct message for a customer requested change for a payment on account 2 of 2" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = PoaTwoDebit, outstandingAmount = 0),
          adjustmentHistory = customerRequestAdjustmentHistory
        ) {
          document.select("tbody tr").size() shouldBe 2
          document.select("tbody tr:nth-child(2) td:nth-child(2)").text() shouldBe secondPaymentOnAccountRequest
        }
      }


      "late payment interest" should {

        "have the correct heading aand caption for a POA 2 late interest charge" in new TestSetup(
          chargeItem = basePoaTwoDebit,
          latePaymentInterestCharge = true
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe poa2InterestHeading
        }

        "have content explaining the definition of a late payment interest charge on account when charge is a POA2" in new TestSetup(
          chargeItem = basePoaTwoDebit,
          latePaymentInterestCharge = true
        ) {
          document.selectById("lpi-poa4").text() shouldBe lpiPoa2TextSentence
          document.selectById("lpi-poa5").text() shouldBe lpiPoaTextParagraph
          document.selectById("lpi-poa6").text() shouldBe "See guidance on the interest rates set by HMRC (opens in new tab)"
        }

        "display only the charge creation item for a payment on account 2 of 2 late interest charge" in new TestSetup(
          chargeItem = basePoaTwoDebit,
          latePaymentInterestCharge = true
        ) {
          document.select("tbody tr").size() shouldBe 1
          document.select("tbody tr td:nth-child(2)").text() shouldBe secondLatePaymentOnAccountInterestCreated
        }

        "have a link to extra charge if it is a poa with an extra charge" in new TestSetup(chargeItem = basePoaTwoDebit, poaExtraChargeLink = Some("testLink")) {
          document.select("#poa-extra-charge-content").text() shouldBe s"$poaExtraChargeText1 $poaExtraChargeTextLink $poaExtraChargeText2"
          document.select("#poa-extra-charge-link").attr("href") shouldBe "testLink"
          document.select("#poa-extra-charge-link").text() shouldBe poaExtraChargeTextLink
        }

        "not have this extra poa charge content if there is no such charge" in new TestSetup(chargeItem = basePoaTwoDebit) {
          document.doesNotHave(Selectors.id("poa-extra-charge-content"))
        }
      }

      "charge is coded out" when {
        "coding out is accepted" should {
          "display the coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(Accepted), originalAmount = 2500.00)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem, adjustmentHistory = codedOutEmptyAdjustmentHistory(2500.00)) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa2CodedOutHeading
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 2
              document.select("#payment-history-table tbody tr").size() shouldBe 1
              document.select("#payment-history-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.poa2CodedOut.text", "2019", "2020")} £2,500.00"
            }
          }
        }
        "coding out is fully collected" should {
          "display the fully collected coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(FullyCollected), originalAmount = 2500.00)
            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem, adjustmentHistory = codedOutEmptyAdjustmentHistory(2500.00)) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe chargeSummaryPoa2CodedOutHeading
              document.getElementById("howIsInterestCalculated.p1").text() shouldBe "’Payments on account’ are two advance payments made towards your next tax bill."
              document.getElementById("howIsInterestCalculated.list").child(0).text() shouldBe "Income Tax"
              document.getElementById("howIsInterestCalculated.list").child(1).text() shouldBe "Class 4 National Insurance contributions (opens in new tab)"
              document.getElementById("howIsInterestCalculated.list").child(1).link.attr("href") shouldBe "https://www.gov.uk/self-employed-national-insurance-rates"
              document.getElementById("howInterestIsCalculated.p2").text() shouldBe "HMRC estimates the total amount based on your previous year’s tax bill. Each payment is half of that amount."
              document.select("#coding-out-notice").text() shouldBe insetPara
              document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
              document.select(".govuk-table").size() shouldBe 2
              document.select("#payment-history-table tbody tr").size() shouldBe 1
              document.select("#payment-history-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.poa2CodedOut.text", "2019", "2020")} £2,500.00"
            }
          }
        }

        "coding out is cancelled" should {
          "display the coded out details" when {

            val codedOutPoaItem = chargeItemModel(transactionType = PoaTwoDebit, codedOutStatus = Some(Cancelled))

            "Coding Out is Enabled" in new TestSetup(codedOutPoaItem) {
              document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
              document.select("h1").text() shouldBe cancelledSaPayeHeading
              document.select("#cancelled-paye-notice").text() shouldBe cancelledPayeTaxCodeInsetText
              document.select(".govuk-table").size() shouldBe 2
              document.select("#payment-history-table tbody tr").size() shouldBe 1
              document.select("#payment-history-table tbody tr").get(0).text() shouldBe
                s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.cancelledPayeSelfAssessment.text", "2018", "2019")} £1,400.00"
            }
          }
        }
      }
    }

    "charge is a POA 1 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaOneReconciliationDebit)) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa1ReconcileHeading
    }
    "charge is a POA 2 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaTwoReconciliationDebit)) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa2ReconcileHeading
    }
    "charge is interest for a POA 1 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaOneReconciliationDebit), latePaymentInterestCharge = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe "Late payment interest for first payment on account: extra amount"

      document.selectById("poa1-extra-charge-p1").text() shouldBe poa1ReconciliationInterestP1("2017", "2018")
      document.selectById("poa-extra-charge-p4").text() shouldBe poa1ReconciliationInterestP2
      document.selectById("poa-extra-charge-p5").text() shouldBe poa1ReconciliationInterestP3

      document.select("tbody tr").size() shouldBe 1
      document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
      document.select("tbody tr td:nth-child(2)").text() shouldBe lpiPoaOne
      document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
    }
    "charge is interest for a POA 2 reconciliation charge" in new TestSetup(chargeItem = chargeItemModel(transactionType = PoaTwoReconciliationDebit), latePaymentInterestCharge = true) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe poa2ReconcileInterestHeading

      document.selectById("poa2-extra-charge-p1").text() shouldBe poa2ReconciliationInterestP1("2017", "2018")
      document.selectById("poa-extra-charge-p4").text() shouldBe poa2ReconciliationInterestP2
      document.selectById("poa-extra-charge-p5").text() shouldBe poa2ReconciliationInterestP3

      document.select("tbody tr").size() shouldBe 1
      document.select("tbody tr td:nth-child(1)").text() shouldBe "15 Jun 2018"
      document.select("tbody tr td:nth-child(2)").text() shouldBe lpiPoaTwo
      document.select("tbody tr td:nth-child(3)").text() shouldBe "£100.00"
    }
    "charge is a late submission penalty" in new TestSetup(chargeItem = chargeItemModel(transactionType = LateSubmissionPenalty)) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe lateSubmissionPenaltyHeading

      document.selectById("LSP-content-1").text() shouldBe lspContent1
      document.selectById("LSP-content-2").text() shouldBe lspContent2
      document.selectById("LSP-content-3").text() shouldBe lspContent3
      document.selectById("LSP-content-4").text() shouldBe lspContent4
      document.selectById("late-submission-penalty-link").attr("href") shouldBe "testLSPUrl"
    }
    "charge is a first late payment penalty" in new TestSetup(chargeItem = chargeItemModel(transactionType = FirstLatePaymentPenalty)) {
      document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      document.select("h1").text() shouldBe firstLatePaymentPenalty

      document.selectById("first-payment-penalty-p1").text() shouldBe lpp1Content1
      document.selectById("first-payment-penalty-p2").text() shouldBe lpp1Content2
      document.selectById("first-payment-penalty-p2").child(0).attr("href") shouldBe "testLPPUrl"
    }

    "charge is a balancing payment" when {

      val baseBalancing = chargeItemModel(transactionType = BalancingCharge)
      val baseBalancingNics2 = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Nics2))
      val baseBalancingAccepted = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Accepted), originalAmount = 2500.00)
      val baseBalancingCancelled = chargeItemModel(transactionType = BalancingCharge, codedOutStatus = Some(Cancelled))

      "is Nics2" should {

        s"have the correct heading snd caption for a $paymentBreakdownNic2 charge with a sub-transaction type" in new TestSetup(
          chargeItem = baseBalancingNics2
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe class2NicHeading
        }

        s"have the correct heading and caption for a $paymentBreakdownNic2 charge with no sub-transaction type" in new TestSetup(
          chargeItem = chargeItemModel(transactionType = BalancingCharge)
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe balancingChargeHeading
        }

        "have a paragraph explaining which tax year the Class 2 NIC is for" in new TestSetup(
          chargeItem = baseBalancingNics2.copy(lpiWithDunningLock = None)
        ) {
          document.getElementById("classTwoNICSContent.p1").text() shouldBe class2NicTaxYear(2018)
        }

        s"display only the charge creation item for a $paymentBreakdownNic2 charge" in new TestSetup(
          chargeItem = baseBalancingNics2
        ) {
          document.select("tbody tr").size() shouldBe 3
          document.select("tbody tr td:nth-child(2)").text() shouldBe "1.0% £80.00 Class 2 National Insurance created"
        }

      }

      "is accepted" should {

        val creditItemCodingOut = baseBalancingAccepted.copy(
          transactionId = "CODINGOUT02")

        "display the coded out details" when {

          "Coding Out BCD is Enabled" in new TestSetup(creditItemCodingOut, adjustmentHistory = codedOutEmptyAdjustmentHistory(2500.00)) {
            document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
            document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
            document.select("#coding-out-notice").text() shouldBe insetPara
            document.select("#codedOutBCDExplanation").text() shouldBe codingOutBCDMessage2017To2018WithStringMessagesArgument
            document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
            document.select(".govuk-table").size() shouldBe 2
            document.select("#payment-history-table tbody tr").size() shouldBe 1
            document.select("#payment-history-table tbody tr").get(0).text() shouldBe s"29 Mar 2018 ${messages("chargeSummary.chargeHistory.created.poa1CodedOut.text", "2019", "2020")} £2,500.00"
          }
        }

        "Scenario were Class2 NICs has been paid and only coding out information needs to be displayed" when {

          "Coding Out is Enabled" in new TestSetup(
            creditItemCodingOut
          ) {
            document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
            document.select("h1").text() shouldBe chargeSummaryCodingOutHeading2017To2018
            document.select("#coding-out-notice").text() shouldBe insetPara
            document.select("#codedOutBCDExplanation").text() shouldBe codingOutBCDMessage2017To2018WithStringMessagesArgument
            document.select("#coding-out-notice-link").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
            document.selectById("payment-history-table tbody tr").child(1).text() shouldBe messages("chargeSummary.chargeHistory.created.codingOut.text", "2019", "2020")
            document.select(".govuk-table tbody tr").size() shouldBe 3
          }

          "Coding Out is Disabled" in new TestSetup(
            creditItemCodingOut.copy(
              taxYear = TaxYear.forYearEnd(2019),
              codedOutStatus = None,
              outstandingAmount = 2500.00,
              originalAmount = 2500.00)
          ) {
            document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
            document.select("h1").text() shouldBe s"$balancingCharge"
            document.select("#coding-out-notice").text() shouldBe ""
            document.select("#coding-out-message").text() shouldBe ""
            document.select("#coding-out-notice-link").attr("href") shouldBe ""
            document.select(".govuk-table").size() shouldBe 2
            document.select(".govuk-table tbody tr").size() shouldBe 3
          }
        }
      }

      "is cancelled" should {
        "have the correct heading and caption for a Cancelled PAYE Self Assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled
        ) {
          document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
          document.select("h1").text() shouldBe cancelledSaPayeHeading
        }

        "have a paragraphs explaining Cancelled PAYE self assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None)
        ) {
          document.select("#cancelled-paye-notice").text() shouldBe cancelledPayeTaxCodeInsetText
          document.select("#cancelled-paye-notice a").attr("href") shouldBe cancelledPayeTaxCodeInsetLink
        }

        "not display the Payment breakdown list for cancelled PAYE self assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None),
          paymentBreakdown = Nil
        ) {
          document.doesNotHave(Selectors.id("heading-payment-breakdown"))
        }

        "display a payment history" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None),
          paymentBreakdown = paymentBreakdown
        ) {
          document.getElementById("charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
        }

        "display only the charge creation item when no history found for cancelled PAYE self assessment" in new TestSetup(
          chargeItem = baseBalancingCancelled.copy(lpiWithDunningLock = None)
        ) {
          document.select("#payment-history-table tbody tr").size() shouldBe 1
          document.select("#payment-history-table tbody tr td:nth-child(1)").text() shouldBe "29 Mar 2018"
          document.select("#payment-history-table tbody tr td:nth-child(2)").text() shouldBe cancelledSaPayeCreated
          document.select("#payment-history-table tbody tr td:nth-child(3)").text() shouldBe "£1,400.00"
        }
      }

      "have the correct heading and caption for a new balancing charge" in new TestSetup(
        chargeItem = baseBalancing.copy(taxYear = TaxYear.forYearEnd(2019))) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe balancingChargeHeading
      }

      "have content explaining the definition of a balancing charge when charge is a balancing charge" in new TestSetup(
        chargeItem = baseBalancing) {
        document.getElementById("whatIsABalancingPayment.p1").text() shouldBe bcdTextParagraph
        document.select(".govuk-details__text ul>:nth-child(1)").text() shouldBe bcdTextBullets
        document.select(".govuk-details__text ul>:nth-child(2)").text() shouldBe bcdTextP2
      }

      "have content explaining the definition of a late balancing charge when charge is a balancing charge" in new TestSetup(
        chargeItem = baseBalancing,
        latePaymentInterestCharge = true) {
        document.selectById("lpi-bcd1").text() shouldBe lpiBcdTextSentence
        document.selectById("lpi-bcd2").text() shouldBe lpiBcdTextParagraph
        document.selectById("lpi-bcd3").text() shouldBe "See guidance on the interest rates set by HMRC (opens in new tab)"
      }

      "have the correct heading and caption for a new balancing charge late interest charge" in new TestSetup(
        chargeItem = baseBalancing.copy(taxYear = TaxYear.forYearEnd(2019)),
        latePaymentInterestCharge = true
      ) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe balancingChargeInterestHeading
      }

      "have the correct heading and caption for an amend balancing charge late interest charge" in new TestSetup(
        chargeItem = baseBalancing.copy(taxYear = TaxYear.forYearEnd(2019)),
        latePaymentInterestCharge = true) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe balancingChargeInterestHeading
      }
    }

    "charge is a MFA debit" should {

      val mfaChargeItem = chargeItemModel(transactionType = MfaDebitCharge)

      val paymentAllocations = List(
        paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01")),
        paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", 1600.0, Some("123456789012"), Some("PAYID01")),
      )
      "have content explaining the definition of a HMRC adjustment when charge is a MFA Debit" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = MfaDebitCharge)) {
        document.getElementById("credit-created-text").text() shouldBe adjustmentText
      }

      "Display an unpaid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019))
      ) {
        val summaryListText = "Due date Overdue 15 May 2019 Amount £1,400.00 Still to pay £1,400.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // payment history should show only "HMRC adjustment created"
        document.select("#payment-history-table tr").size shouldBe 2
        document.select("#payment-history-table tr").text() shouldBe paymentHistoryText
      }

      "Display a paid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019), outstandingAmount = 0.0),
        paymentAllocations = paymentAllocations
      ) {
        val summaryListText = "Due date 15 May 2019 Amount £1,400.00 Still to pay £0.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        val MFADebitAllocation1 = "30 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2021 £1,500.00"
        val MFADebitAllocation2 = "31 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2021 £1,600.00"
        val allocationLinkHref = financialsRoutes.PaymentAllocationsController.viewPaymentAllocation("PAYID01").url
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // payment history should show two rows "HMRC adjustment created" and "payment put towards HMRC Adjustment"
        document.select("#payment-history-table tr").size shouldBe 4
        document.select("#payment-history-table tr:nth-child(1)").text() shouldBe paymentHistoryText
        document.select("#payment-history-table tr:nth-child(2)").text() shouldBe MFADebitAllocation1
        document.select("#payment-history-table tr:nth-child(3)").text() shouldBe MFADebitAllocation2
        // allocation link should be to agent credits in 2018 screen
        document.select("#payment-history-table tr:nth-child(3) a").attr("href") shouldBe allocationLinkHref
      }
    }

    "payment breakdown" should {
      "not display a notification banner when there are no dunning locks" in new TestSetup(
        chargeItem = chargeItemModel().copy(lpiWithDunningLock = None),
        paymentBreakdown = paymentBreakdown
      ) {
        document.doesNotHave(Selectors.id("govuk-notification-banner-title"))
      }

      "display a notification banner when there are dunning locks" which {

        s"has the '$dunningLockBannerHeader' heading" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithDunningLocks
        ) {
          document.selectById("govuk-notification-banner-title").text() shouldBe dunningLockBannerHeader
        }

        "has the link for Payment under review which opens in new tab" in new TestSetup(
          chargeItem = chargeItemModel(),
          paymentBreakdown = paymentBreakdownWithDunningLocks
        ) {
          val link: Elements = document.select(".govuk-notification-banner").select(Selectors.link)

          link.text() shouldBe dunningLockBannerLink
          link.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          link.attr("target") shouldBe "_blank"
        }

        "shows the same remaining amount and a due date as in the charge summary list" which {
          "display a remaining amount" in new TestSetup(
            chargeItem = chargeItemModel(outstandingAmount = 1600),
            paymentBreakdown = paymentBreakdownWithDunningLocks
          ) {
            document.select(".govuk-notification-banner")
              .select(".govuk-notification-banner__content").text() shouldBe dunningLockBannerText("£1,600.00", "15 May 2019")
          }

          "display 0 if a cleared amount equal to the original amount is present but an outstanding amount is not" in new TestSetup(
            chargeItem = chargeItemModel().copy(outstandingAmount = 0),
            paymentBreakdown = paymentBreakdownWithDunningLocks
          ) {
            document.select(".govuk-notification-banner")
              .select(".govuk-notification-banner__content").text() shouldBe dunningLockBannerText("£0.00", "15 May 2019")
          }
        }
      }
    }

    "have a fallback link" in new TestSetup() {
      document.hasFallbackBacklink
    }


    "not display the Payment breakdown list when payments breakdown is empty" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = Nil) {
      document.doesNotHave(Selectors.id("heading-payment-breakdown"))
    }

    "display the Payment breakdown list" which {

      "has a correct heading and caption" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ) {
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2018)
      }

    }


    "have a interest lock payment link when the interest is accruing" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWhenInterestAccrues
    ) {
      document.select("#what-you-owe-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-link").attr("href") shouldBe financialsRoutes.WhatYouOweController.show().url
    }

    "have a interest lock payment link when the interest has previously" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest
    ) {
      document.select("#what-you-owe-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-link").attr("href") shouldBe financialsRoutes.WhatYouOweController.show().url
    }

    "have no interest lock payment link when there is no accrued interest" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest
    ) {
      document.select("#what-you-owe-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-link").attr("href") shouldBe financialsRoutes.WhatYouOweController.show().url
    }

    "have no interest lock payment link when there is an intererst lock but no accrued interest" in new TestSetup(
      chargeItem = chargeItemModel(lpiWithDunningLock = None),
      paymentBreakdown = paymentBreakdownWithOnlyInterestLock
    ) {
      document.select("#what-you-owe-link").text() shouldBe interestLinkText
      document.select("#what-you-owe-link").attr("href") shouldBe financialsRoutes.WhatYouOweController.show().url
    }

    "charge history" should {
      "display a charge history heading as an h2 when there is no Payment Breakdown" in new TestSetup(
        chargeItem = chargeItemModel().copy(outstandingAmount = 0, lpiWithDunningLock = None)
      ) {
        document.select("#charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
      }

      "display a charge history heading as an h3 when there is a Payment Breakdown" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ) {
        document.select("#charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
      }

      "display charge history heading as poa1 heading when charge is a poa1" in new TestSetup(
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdown
      ){
        document.select("#charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
      }

      "display charge history heading as poa2 heading when charge is a poa2" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = PoaTwoDebit),
        paymentBreakdown = paymentBreakdown
      ){
        document.select("#charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
      }

      "display charge history heading as non-poa heading when charge is not a poa" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = MfaDebitCharge),
        paymentBreakdown = paymentBreakdown
      ){
        document.select("#charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
      }

      "display charge history heading as late payment interest history when charge is a late payment interest" in new TestSetup(
        chargeItem = chargeItemModel(transactionType = PoaTwoDebit),
        paymentBreakdown = paymentBreakdown,
        latePaymentInterestCharge = true
      ){
        document.select("#charge-history-heading").text shouldBe chargeHistoryHeadingGeneric
      }

      "show payment allocations in history table when allocations are credits" when {

        "credit allocations are present in the list" when {

          val paymentAllocations = List(
            paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", -1500.0, Some("123456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", -1600.0, Some("223456789012"), Some("PAYID01"), taxYear = Some("2026")),

            paymentsForCharge(typePOA2, ITSA_WALES, "2018-04-01", -2400.0, Some("323456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typePOA2, NIC4_GB, "2018-04-15", -2500.0, Some("423456789012"), Some("PAYID01"), taxYear = Some("2026")),

            paymentsForCharge(typeBalCharge, ITSA_ENGLAND_AND_NI, "2019-12-10", -3400.0, Some("523456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typeBalCharge, NIC4_NI, "2019-12-11", -3500.0, Some("623456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typeBalCharge, NIC2_WALES, "2019-12-12", -3600.0, Some("723456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typeBalCharge, CGT, "2019-12-13", -3700.0, Some("823456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typeBalCharge, SL, "2019-12-14", -3800.0, Some("923456789012"), Some("PAYID01"), taxYear = Some("2026")),
            paymentsForCharge(typeBalCharge, VOLUNTARY_NIC2_GB, "2019-12-15", -3900.0, Some("023456789012"), Some("PAYID01"), taxYear = Some("2026")),
          )

          "chargeHistory enabled with a matching link to the credits summary page when there isn't a charge breakdown page for a credit" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = true, paymentAllocations = paymentAllocations, payments = creditsWithoutBreakdownPages) {
            document.select(Selectors.table).select("a").size shouldBe 10
            document.select(Selectors.table).select("a").forall(_.attr("href") == financialsRoutes.CreditsSummaryController.showCreditsSummary(2026).url) shouldBe true
          }

          "chargeHistory enabled with a matching link to charge summary breakdown page for a credit when it's present" in new TestSetup(chargeItem = chargeItemModel(),
            chargeHistoryEnabled = true, paymentAllocations = paymentAllocations, payments = creditsWithBreakdownPages) {
            document.select(Selectors.table).select("a").size shouldBe 10
            document.select(Selectors.table).select("a").forall(_.attr("href") == financialsRoutes.ChargeSummaryController.show(2026, "PAYID01").url) shouldBe true
          }
        }
      }

    }

    "on the ChargeSummary" should {
      "display the notification banner when there is a dunning lock present" in new TestSetup(
        isAgent = false,
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdownWithDunningLocks
      ) {
        document.selectById("govuk-notification-banner-title")
          .select(Selectors.h2).text() shouldBe dunningLockBannerHeader
      }
    }
  }

  "The charge summary view when missing mandatory expected fields" should {
    "throw a MissingFieldException" in new TestSetup(chargeItem = chargeItemModel()) {
      val exceptionViewModel: ChargeSummaryViewModel = ChargeSummaryViewModel(
        currentDate = dateService.getCurrentDate,
        chargeItem = chargeItemModel(dueDate = None),
        backUrl = "testBackURL",
        gatewayPage = None,
        paymentBreakdown = paymentBreakdown,
        paymentAllocations = List(),
        payments = payments,
        chargeHistoryEnabled = true,
        latePaymentInterestCharge = false,
        reviewAndReconcileCredit = None,
        penaltiesEnabled = true,
        poaOneChargeUrl = "",
        poaTwoChargeUrl = "",
        adjustmentHistory = defaultAdjustmentHistory,
        LSPUrl = "",
        LPPUrl = "",
        taxYearSummaryUrl = (taxYear: Int) => appConfig.taxYearSummaryUrl(isAgent = false, taxYear = taxYear)
      )
      val thrownException = intercept[MissingFieldException] {

        chargeSummary(exceptionViewModel, None, financialsRoutes.WhatYouOweController.show().url, false)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Due Date"
    }
  }

  "agent" when {

    "render the row for the charge" should {
      "charge is a Review and Reconcile credit for Payment on Account 1" in new TestSetup(
        isAgent = true,
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaOneReconciliationCredit)
      ) {
        document.selectById("rar-charge-link").text() shouldBe "First payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe financialsRoutes.ChargeSummaryController.showAgent(2020, "some-id").url

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(5)
          .text() shouldBe "£1,000.00"

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(3)
          .text() shouldBe "6 Aug 2018"
      }
      "charge is a Review and Reconcile credit for Payment on Account 2" in new TestSetup(
        isAgent = true,
        reviewAndReconcileCredit = reviewAndReconcileCreditChargeItem(PoaTwoReconciliationCredit)
      ) {
        document.selectById("rar-charge-link").text() shouldBe "Second payment on account: credit from your tax return"
        document.selectById("rar-charge-link").attr("href") shouldBe financialsRoutes.ChargeSummaryController.showAgent(2020, "some-id").url

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(5)
          .text() shouldBe "£1,000.00"

        document
          .getElementById("payment-history-table")
          .getElementsByClass("govuk-table__body")
          .first()
          .getElementsByClass("govuk-table__cell")
          .get(3)
          .text() shouldBe "6 Aug 2018"
      }
    }

    "The charge summary view" should {

      "have a interest lock payment link when the interest is accruing" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWhenInterestAccrues, isAgent = true,
        whatYouOweUrl = financialsRoutes.WhatYouOweController.showAgent().url) {
        document.select("#what-you-owe-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-link-agent").attr("href") shouldBe whatYouOweAgentUrl
      }

      "have a interest lock payment link when the interest has previously" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithPreviouslyAccruedInterest, isAgent = true,
        whatYouOweUrl = financialsRoutes.WhatYouOweController.showAgent().url) {
        document.select("#what-you-owe-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-link-agent").attr("href") shouldBe whatYouOweAgentUrl
      }

      "have no interest lock payment link when there is no accrued interest" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyAccruedInterest, isAgent = true,
        whatYouOweUrl = financialsRoutes.WhatYouOweController.showAgent().url) {
        document.select("#what-you-owe-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-link-agent").attr("href") shouldBe whatYouOweAgentUrl
      }

      "have no interest lock payment link when there is an intererst lock but no accrued interest" in new TestSetup(chargeItem = chargeItemModel(lpiWithDunningLock = None), paymentBreakdown = paymentBreakdownWithOnlyInterestLock, isAgent = true,
        whatYouOweUrl = financialsRoutes.WhatYouOweController.showAgent().url) {
        document.select("#what-you-owe-link-agent").text() shouldBe interestLinkTextAgent
        document.select("#what-you-owe-link-agent").attr("href") shouldBe whatYouOweAgentUrl
      }

      "does not have any payment lock notes or link when there is no interest locks on the page " in new TestSetup(chargeItem = chargeItemModel(), paymentBreakdown = paymentBreakdown, isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "not have a payment link when there is an outstanding amount of 0" in new TestSetup(
        chargeItem = chargeItemModel(outstandingAmount = 0.0),
        isAgent = true) {
        document.select("div#payment-link-2018").text() shouldBe ""
      }

      "list payment allocations with right number of rows and agent payment allocations link" in new TestSetup(chargeItem = chargeItemModel(),
        chargeHistoryEnabled = true, paymentAllocations = List(
          paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01"))), isAgent = true) {
        document.select(Selectors.table).select("a").size shouldBe 1
        document.select(Selectors.table).select("a").forall(_.attr("href") == financialsRoutes.PaymentAllocationsController.viewPaymentAllocationAgent("PAYID01").url) shouldBe true
      }
    }

    "MFA Credits" when {

      val paymentAllocations = List(
        paymentsForCharge(typePOA1, ITSA_NI, "2018-03-30", 1500.0, Some("123456789012"), Some("PAYID01")),
        paymentsForCharge(typePOA1, NIC4_SCOTLAND, "2018-03-31", 1600.0, Some("123456789012"), Some("PAYID01"))
      )

      val mfaChargeItem = chargeItemModel(transactionType = MfaDebitCharge)

      "Display an unpaid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019)),
        isAgent = true) {
        val summaryListText = "Due date Overdue 15 May 2019 Amount £1,400.00 Still to pay £1,400.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // payment history should show only "HMRC adjustment created"
        document.select("#payment-history-table tr").size shouldBe 2
        document.select("#payment-history-table tr").text() shouldBe paymentHistoryText
      }

      "Display a paid MFA Credit" in new TestSetup(
        chargeItem = mfaChargeItem.copy(taxYear = TaxYear.forYearEnd(2019), outstandingAmount = 0.0), isAgent = true,
        paymentAllocations = paymentAllocations) {
        val summaryListText = "Due date 15 May 2019 Amount £1,400.00 Still to pay £0.00 "
        val hmrcCreated = messages("chargeSummary.chargeHistory.created.hmrcAdjustment.text")
        val paymentHistoryText = "Date Description Amount 29 Mar 2018 " + hmrcCreated + " £1,400.00"
        val MFADebitAllocation1 = "30 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2021 £1,500.00"
        val MFADebitAllocation2 = "31 Mar 2018 " + messages("chargeSummary.paymentAllocations.mfaDebit") + " 2021 £1,600.00"
        val allocationLinkHref = financialsRoutes.PaymentAllocationsController.viewPaymentAllocationAgent("PAYID01").url
        // heading should be hmrc adjustment
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe poa1Caption(2019)
        document.select("h1").text() shouldBe messages("chargeSummary.hmrcAdjustment.text")
        // payment history should show two rows "HMRC adjustment created" and "payment put towards HMRC Adjustment"
        document.select("#payment-history-table tr").size shouldBe 4

        document.select("#payment-history-table tr:nth-child(1)").text() shouldBe paymentHistoryText
        document.select("#payment-history-table tr:nth-child(2)").text() shouldBe MFADebitAllocation1
        document.select("#payment-history-table tr:nth-child(3)").text() shouldBe MFADebitAllocation2
        // allocation link should be to agent payment allocation page
        document.select("#payment-history-table tr:nth-child(3) a").attr("href") shouldBe allocationLinkHref
      }
    }

    "on the ChargeSummary page" should {
      "display the notification banner when there is a dunning lock present" in new TestSetup(
        isAgent = true,
        chargeItem = chargeItemModel(),
        paymentBreakdown = paymentBreakdownWithDunningLocks
      ) {
        document.selectById("govuk-notification-banner-title")
          .select(Selectors.h2).text() shouldBe dunningLockBannerHeader
      }
    }
  }


  "ChargeSummaryView" when {
    "charge is an ITSAReturnAmendment type and has a charge classification of 'RA'" should {
      "display the correct content" in new TestSetupv2(chargeItem = chargeItemModel(transactionType = ITSAReturnAmendment, chargeClassification = Some("RA"))) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe itsaEnquiryAmendmentHeading
        document.getElementById("itsaEnquiryAmendment.p1").text() shouldBe itsaEnquiryAmendmentParagraph1(2017, 2018)
        document.getElementById("itsaEnquiryAmendment.link").attr("href") shouldBe viewModel.taxYearSummaryUrl(2018)
        document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
        document.getElementById("charge-history-caption").text() shouldBe "This extra amount goes towards your 2017 to 2018 tax bill."
        document.select("#payment-history-table > tbody > tr > td:nth-child(2)").text() shouldBe enquiryAmendmentDescriptionText
      }
    }
    "charge is an ITSAReturnAmendmentCredit type and has a charge classification of 'RA'" should {
      "display the correct content" in new TestSetupv2(chargeItem = chargeItemModel(transactionType = ITSAReturnAmendmentCredit, chargeClassification = Some("RA"))) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe itsaEnquiryAmendmentCreditHeading
        Option(document.getElementById("itsa-enquiry-amendment-credit-p1")).isDefined shouldBe true
        Option(document.getElementById("itsa-enquiry-amendment-credit-p2")).isDefined shouldBe true
      }
    }

    val correctionCreditHeading: String = "Credit from HMRC correction to your tax return"
    "charge is an ITSAReturnAmendmentCredit type and has a charge classification of 'MC'" should {
      "display the correct content" in new TestSetupv2(chargeItem = chargeItemModel(transactionType = ITSAReturnAmendmentCredit, chargeClassification = Some("MC"))) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe correctionCreditHeading
        Option(document.getElementById("itsa-return-correction-credit-p1")).isDefined shouldBe true
        Option(document.getElementById("itsa-return-correction-credit-p2")).isDefined shouldBe true
      }
    }

    "charge is an ITSAReturnAmendmentCredit type and has a charge classification of 'AC'" should {
      "display the correct content" in new TestSetupv2(chargeItem = chargeItemModel(transactionType = ITSAReturnAmendmentCredit, chargeClassification = Some("AC"))) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe correctionCreditHeading
        val p1 = document.getElementById("itsa-return-correction-credit-p1").text()
        p1 should include("HMRC corrected your tax return. This changed your tax calculation for")
        p1 should include(", resulting in a credit being added to your account.")

        document.getElementById("itsa-return-correction-credit-p2").text() shouldBe "This credit may be used automatically by HMRC to cover your future tax bills when they become due."
      }
    }

    "charge is an ITSAOverRepayment" should {
      "display the correct content" in new TestSetupv2(chargeItem = chargeItemModel(transactionType = ITSAOverRepayment)) {
        document.getElementsByClass("govuk-heading-xl").text() shouldBe itsaOverRepaymentHeading
        document.getElementById("itsa-over-repayment-p1").text() shouldBe itsaOverRepaymentParagraph1
        document.getElementById("itsa-over-repayment-p2").text() shouldBe itsaOverRepaymentParagraph2
        document.select("#payment-history-table > tbody > tr > td:nth-child(2)").text() shouldBe itsaOverRepaymentDescriptionText
      }
    }

    "charge is suspended with dunning lock 'Stand over order'" should {
      "display the correct content" in new TestSetupv2(
        chargeItem = chargeItemModel(dunningLock = true),
        paymentBreakdown =
          List(
            FinancialDetail(
              TaxYear.forYearEnd(2018).shortenTaxYearEnd,
              Some("SA POA 1 Reconciliation Debit"),
              Some("4920"),
              Some(id1040000123),
              Some(LocalDate.parse("2018-08-16")),
              Some("POA1"),
              Some("SA POA 1 Reconciliation Debit"),
              Some(1400.00),
              Some(1400.00),
              Some(1400.00),
              Some(0),
              Some(ITSA_ENGLAND_AND_NI),
              None,
              Some(
                Seq(
                  SubItem(
                    dueDate = Some(LocalDate.of(2019, 5, 15)),
                    subItemId = Some("001"),
                    amount = Some(BigDecimal("1400.00")),
                    dunningLock = Some("Stand over order")
                  )
                )
              )
            )
          )
      ) {

        document.select("#govuk-notification-banner-title").size() shouldBe 1
        document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
        document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe dunningLocksBannerHeading
        document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountHeading
        document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
        document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
        document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdYourFirstPaymentOnAccountText
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "15 May 2019"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe createdFirstLatePaymentPenaltyStoodOverText
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£1,400.00"
      }

      "display the correct content for the Balancing Payment" in new TestSetupv2(
        chargeItem = chargeItemModel(
          transactionType = BalancingCharge,
          originalAmount = BigDecimal(2100.00),
          outstandingAmount = BigDecimal(2100.00),
          dunningLock = true,
          totalSoAmt = Some(BigDecimal(2100.00))),
        adjustmentHistory = AdjustmentHistoryModel(AdjustmentModel(2100, Some(LocalDate.of(2018, 3, 29)), AdjustmentReversalReason), List()),
        paymentBreakdown =
          List(
            FinancialDetail(
              TaxYear.forYearEnd(2018).shortenTaxYearEnd,
              Some("Balancing Payment"),
              Some("4910"),
              Some(id1040000123),
              Some(LocalDate.parse("2018-08-16")),
              Some("Nics2"),
              Some("ITSA England & NI"),
              Some(2100.00),
              Some(2100.00),
              Some(2100.00),
              Some(0),
              Some(ITSA_ENGLAND_AND_NI),
              None,
              Some(
                Seq(
                  SubItem(
                    dueDate = Some(LocalDate.of(2020, 1, 31)),
                    subItemId = Some("001"),
                    amount = Some(BigDecimal("2100.00"))
                  ),
                  SubItem(
                    dueDate = Some(LocalDate.of(2019, 7, 31)),
                    subItemId = Some("002"),
                    amount = Some(BigDecimal("2100.00")),
                    dunningLock = Some("Stand over order")
                  )
                )
              )
            )
          )
      ) {

        document.select("#govuk-notification-banner-title").size() shouldBe 1
        document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
        document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe dunningLocksBannerHeading
        document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountHeading
        document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
        document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
        document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdBalancingPaymentText
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£2,100.00"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "31 Jul 2019"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe createdFirstLatePaymentPenaltyStoodOverText
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£2,100.00"
      }

      "display the correct content when first lpp with dunningLock" in new TestSetupv2(
        chargeItem = financialDetailsLatePaymentPenalties.head.copy(dunningLock = true)
      ) {
        document.select("#govuk-notification-banner-title").size() shouldBe 1
        document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
        document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe dunningLocksBannerHeading
        document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountHeading
        document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
        document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
        document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2022 to 2023 tax bill."
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe firstLatePaymentPenaltyText
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe chargeSummaryNoDataText
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe createdFirstLatePaymentPenaltyStoodOverText
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£43.21"
      }
    }

    "charge is suspended with dunning lock 'Formal Standover'" should {
      "display the correct content" in new TestSetupv2(
        chargeItem = chargeItemModel(dunningLock = true, isRevenueAmendment = true, chargeClassification = Some("RA"), dunningLockValue = Some("Formal Standover"), totalSoAmt = Some(BigDecimal("1400.00"))),
        paymentBreakdown =
          List(
            FinancialDetail(
              TaxYear.forYearEnd(2018).shortenTaxYearEnd,
              Some("ITSA Return Amendment Debit"),
              Some("4915"),
              Some(id1040000123),
              Some(LocalDate.parse("2018-08-16")),
              Some("POA1"),
              Some("ITSA Return Amendment Debit"),
              Some(1400.00),
              Some(1400.00),
              Some(1400.00),
              Some(0),
              Some(ITSA_ENGLAND_AND_NI),
              None,
              Some(
                Seq(
                  SubItem(
                    dueDate = Some(LocalDate.of(2019, 5, 15)),
                    subItemId = Some("001"),
                    amount = Some(BigDecimal("1400.00")),
                    dunningLock = Some("Formal Standover")
                  )
                )
              )
            )
          )
      ) {

        document.select("#govuk-notification-banner-title").size() shouldBe 1
        document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
        document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe formalDunningLocksBannerHeading
        document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountHeading
        document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
        document.doesNotHave("govuk-warning-text__text")
        document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdYourFirstPaymentOnAccountText
        document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "15 May 2019"
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe formalStandoverChargeHistoryText
        document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£1,400.00"
      }
    }

    "ChargeSummaryView" when {

      "'StoodOverCharges' FS is true " should {

        "'isPartialSuspendedCharge' param is false" when {

          "partial 'informal' charge is suspended with dunning lock 'collectable breakdown page'" should {
            "display the correct content" in new TestSetupv2(
              chargeItem = chargeItemModel(
                totalSoAmt = Some(BigDecimal(3500)),
                collectableAmt = Some(BigDecimal(1250)),
                dunningLock = true,
                dunningLockValue = Some("Stand over order")
              ),
              paymentBreakdown = financialDetailsWithDunningLock(),
              isStoodOverEnabled = true,
              mockFakeRequest = fakeRequestWithIsPartialSuspendedChargeFalse
            ) {
              document.select("#govuk-notification-banner-title").size() shouldBe 1
              document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
              document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe partialInformalCollectableBannerHeading("£3,500.00")
              document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountOverDueChargeHeading
              document.getElementById("charge-amount-heading").text().split(" ").filter(_.startsWith("£")).head.trim shouldBe "£1,250.00"
              document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
              document.getElementsByClass("govuk-warning-text__text").text() shouldBe warningText
              document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdYourFirstPaymentOnAccountText
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "15 May 2019"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe createdFirstLatePaymentPenaltyStoodOverText
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£3,500.00"
            }
          }

          "partial 'formal' charge is suspended with dunning lock 'collectable breakdown page'" should {
            "display the correct content" in new TestSetupv2(
              chargeItem = chargeItemModel(
                isRevenueAmendment = true,
                chargeClassification = Some("RA"),
                dunningLockValue = Some("Formal Standover"),
                totalSoAmt = Some(BigDecimal(3500)),
                collectableAmt = Some(BigDecimal(1250)),
                dunningLock = true),
              paymentBreakdown = financialDetailsWithDunningLock(Some("Formal Standover")),
              isStoodOverEnabled = true,
              mockFakeRequest = fakeRequestWithIsPartialSuspendedChargeFalse
            ) {
              document.select("#govuk-notification-banner-title").size() shouldBe 1
              document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
              document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe partialFormalCollectableBannerHeading("£3,500.00")
              document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountOverDueChargeHeading
              document.getElementById("charge-amount-heading").text().split(" ").filter(_.startsWith("£")).head.trim shouldBe "£1,250.00"
              document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
              document.doesNotHave("govuk-warning-text__text")
              document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdYourFirstPaymentOnAccountText
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "15 May 2019"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe formalStandoverChargeHistoryText
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£3,500.00"
            }
          }
        }

        "'isPartialSuspendedCharge' param is true" when {

          "partial 'informal' charge is suspended with dunning lock 'stood over breakdown page'" should {
            "display the correct content" in new TestSetupv2(
              chargeItem = chargeItemModel(
                totalSoAmt = Some(BigDecimal(3500)),
                collectableAmt = Some(BigDecimal(1250)),
                dunningLock = true,
                dunningLockValue = Some("Stand over order")
              ),
              paymentBreakdown = financialDetailsWithDunningLock(),
              isStoodOverEnabled = true,
              mockFakeRequest = fakeRequestWithIsPartialSuspendedChargeTrue
            ) {
              document.select("#govuk-notification-banner-title").size() shouldBe 1
              document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
              document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe partialInformalStoodOverBannerHeading
              document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountHeading
              document.getElementById("charge-amount-heading").text().split(" ").filter(_.startsWith("£")).head.trim shouldBe "£3,500.00"
              document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
              document.doesNotHave("govuk-warning-text__text")
              document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdYourFirstPaymentOnAccountText
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "15 May 2019"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe createdFirstLatePaymentPenaltyStoodOverText
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£3,500.00"
            }
          }

          "partial 'formal' charge is suspended with dunning lock 'stood over breakdown page'" should {
            "display the correct content" in new TestSetupv2(
              chargeItem = chargeItemModel(
                isRevenueAmendment = true,
                chargeClassification = Some("RA"),
                dunningLockValue = Some("Formal Standover"),
                totalSoAmt = Some(BigDecimal(3500)),
                collectableAmt = Some(BigDecimal(1250)),
                dunningLock = true),
              paymentBreakdown = financialDetailsWithDunningLock(Some("Formal Standover")),
              isStoodOverEnabled = true,
              mockFakeRequest = fakeRequestWithIsPartialSuspendedChargeTrue
            ) {

              document.select("#govuk-notification-banner-title").size() shouldBe 1
              document.getElementsByClass("govuk-notification-banner__title").first.text() shouldBe dunningLocksBannerTitle
              document.getElementsByClass("govuk-notification-banner__content").first.text() shouldBe partialFormalStoodOverBannerHeading
              document.getElementById("charge-amount-heading").text().split("£").head.trim shouldBe chargeAmountHeading
              document.getElementById("charge-amount-heading").text().split(" ").filter(_.startsWith("£")).head.trim shouldBe "£3,500.00"
              document.getElementById("charge-history-heading").text() shouldBe firstPaymentOnAccountHeading
              document.doesNotHave("govuk-warning-text__text")
              document.getElementById("charge-history-caption").text() shouldBe "This charge goes towards your 2017 to 2018 tax bill."
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(1)").text() shouldBe "29 Mar 2018"
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(2)").text() shouldBe createdYourFirstPaymentOnAccountText
              document.select("#payment-history-table > tbody > tr:nth-child(1) > td:nth-child(3)").text() shouldBe "£1,400.00"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(1)").text() shouldBe "15 May 2019"
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(2)").text() shouldBe formalStandoverChargeHistoryText
              document.select("#payment-history-table > tbody > tr:nth-child(2) > td:nth-child(3)").text() shouldBe "£3,500.00"
            }
          }
        }
      }
    }
  }
}