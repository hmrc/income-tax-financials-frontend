/*
 * Copyright 2024 HM Revenue & Customs
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

package common.models.admin

import play.api.libs.json._
import play.api.mvc.PathBindable

import scala.collection.immutable

case class FeatureSwitch(name: FeatureSwitchName, isEnabled: Boolean)

object FeatureSwitch {
  implicit val format: OFormat[FeatureSwitch] = Json.format[FeatureSwitch]
}

sealed trait FeatureSwitchName {
  val name: String
}

object FeatureSwitchName {

  implicit val writes: Writes[FeatureSwitchName] = (o: FeatureSwitchName) => JsString(o.name)

  implicit val reads: Reads[FeatureSwitchName] = {
    case JsString(ChargeHistory.name) =>
      JsSuccess(ChargeHistory)
    case JsString(CreditsRefundsRepay.name) =>
      JsSuccess(CreditsRefundsRepay)
    case JsString(PaymentHistoryRefunds.name) =>
      JsSuccess(PaymentHistoryRefunds)
    case JsString(PenaltiesAndAppeals.name) =>
      JsSuccess(PenaltiesAndAppeals)
    case JsString(SelfServeTimeToPayR17.name) =>
      JsSuccess(SelfServeTimeToPayR17)
    case JsString(TriggeredMigration.name) =>
      JsSuccess(TriggeredMigration)
    case JsString(SubmitClaimToAdjustToNrs.name) =>
      JsSuccess(SubmitClaimToAdjustToNrs)
    case JsString(`CY+1YouMustWaitToSignUpPageEnabled`.name) =>
      JsSuccess(`CY+1YouMustWaitToSignUpPageEnabled`)
    case JsString(ObligationsFrontend.name) =>
      JsSuccess(ObligationsFrontend)
    case JsString(NoIncomeSourcesRedirect.name) =>
      JsSuccess(NoIncomeSourcesRedirect)
    case JsString(BusinessDetailsFrontend.name) =>
      JsSuccess(BusinessDetailsFrontend)
    case invalidName =>
      JsSuccess(InvalidFS)
  }

  implicit val formats: Format[FeatureSwitchName] =
    Format(reads, writes)

  implicit def pathBindable: PathBindable[FeatureSwitchName] = new PathBindable[FeatureSwitchName] {

    override def bind(key: String, value: String): Either[String, FeatureSwitchName] =
      JsString(value).validate[FeatureSwitchName] match {
        case JsSuccess(name, _) =>
          Right(name)
        case _ =>
          Left(s"The feature switch `$value` does not exist")
      }

    override def unbind(key: String, value: FeatureSwitchName): String =
      value.name
  }

  val allFeatureSwitches: immutable.Set[FeatureSwitchName] =
    Set(
      ChargeHistory,
      CreditsRefundsRepay,
      ObligationsFrontend,
      PaymentHistoryRefunds,
      PenaltiesAndAppeals,
      SubmitClaimToAdjustToNrs,
      SelfServeTimeToPayR17,
      TriggeredMigration,
      `CY+1YouMustWaitToSignUpPageEnabled`,
      NoIncomeSourcesRedirect,
      BusinessDetailsFrontend
    )

  def get(str: String): Option[FeatureSwitchName] = allFeatureSwitches find (_.name == str)
}

case object ChargeHistory extends FeatureSwitchName {
  override val name: String = "charge-history"
  override def toString: String = "Charge History"
}

case object CreditsRefundsRepay extends FeatureSwitchName {
  override val name = "credits-refunds-repay"
  override def toString: String = "Credits/Refunds Repayment"
}

case object InvalidFS extends FeatureSwitchName {
  override val name: String = "invalid-feature-switch"
  override def toString: String = "Invalid feature Switch"
}

case object ObligationsFrontend extends FeatureSwitchName {
  override val name: String = "obligations-frontend"
  override def toString: String = "Obligations Frontend"
}

case object PaymentHistoryRefunds extends FeatureSwitchName {
  override val name = "payment-history-refunds"
  override def toString: String = "Payment History Refunds"
}

case object PenaltiesAndAppeals extends FeatureSwitchName {
  override val name: String = "penalties-and-appeals"
  override def toString: String = "Penalties and Appeals"
}

case object SelfServeTimeToPayR17 extends FeatureSwitchName {
  override val name: String = "self-serve-time-to-pay-r17"
  override def toString: String = "Self Serve Time To Pay R17"
}

case object TriggeredMigration extends FeatureSwitchName {
  override val name: String = "triggered-migration"
  override def toString: String = "Triggered Migration"
}

case object SubmitClaimToAdjustToNrs extends FeatureSwitchName {
  override val name: String = "submit-claim-to-adjust-to-nrs"
  override def toString: String = "Submit Claim to Adjust to NRS"
}

case object `CY+1YouMustWaitToSignUpPageEnabled` extends FeatureSwitchName {
  override val name: String = "cy-plus-one-you-must-wait-to-sign-up-page-enabled"
  override def toString: String = "CY+1 You Must Wait To Sign Up Page Enabled"
}

case object NoIncomeSourcesRedirect extends FeatureSwitchName {
  override val name: String = "no-income-sources-redirect"
  override def toString: String = "No Income Sources Redirect"
}

case object BusinessDetailsFrontend extends FeatureSwitchName {
  override val name: String = "business-details-frontend"
  override val toString: String = "Business Details Frontend"
}
