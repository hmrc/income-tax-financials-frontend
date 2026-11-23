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

package controllers

import auth.{AuthActions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.GatewayPage.GatewayPage
import implicits.ImplicitDateFormatterImpl
import models.admin.CreditsRefundsRepay
import models.core.Nino
import services.AuditingService
import _root_.app.controllers.agent.errors.routes as agentErrorRoutes
import _root_.app.controllers.errors.routes as errorRoutes
import forms.utils.SessionKeys.gatewayPage
import models.audit.PaymentAllocationsResponseAuditModel
import models.paymentAllocationCharges.{PaymentAllocationError, PaymentAllocationViewModel}
import services.PaymentAllocationsService
import utils.FallBackBackLinks
import views.html.PaymentAllocationView
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.mvc.Http
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PaymentAllocationsController @Inject()(val paymentAllocationView: PaymentAllocationView,
                                             val authActions: AuthActions,
                                             itvcErrorHandler: ItvcErrorHandler,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             paymentAllocations: PaymentAllocationsService,
                                             auditingService: AuditingService)
                                            (implicit val mcc: MessagesControllerComponents,
                                             val ec: ExecutionContext,
                                             val implicitDateFormatter: ImplicitDateFormatterImpl,
                                             val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching with FallBackBackLinks {

  private lazy val redirectUrlIndividual: String = errorRoutes.NotFoundDocumentIDLookupController.show().url
  private lazy val redirectUrlAgent: String = agentErrorRoutes.AgentNotFoundDocumentIDLookupController.show().url

  def viewPaymentAllocation(documentNumber: String, origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(
        itvcErrorHandler = itvcErrorHandler,
        documentNumber = documentNumber,
        redirectUrl = redirectUrlIndividual,
        isAgent = false,
        origin = origin
      )

  }

  def handleRequest(itvcErrorHandler: ShowInternalServerError,
                    documentNumber: String,
                    redirectUrl: String,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val sessionGatewayPage = user.session.get(gatewayPage).map(GatewayPage(_))
    paymentAllocations.getPaymentAllocation(Nino(user.nino), documentNumber) map {
      case Right(paymentAllocations: PaymentAllocationViewModel) =>
        val taxYearOpt = paymentAllocations.originalPaymentAllocationWithClearingDate.headOption.flatMap(_.allocationDetail.flatMap(_.getTaxYearOpt))
        val backUrl = getPaymentAllocationBackUrl(isAgent, sessionGatewayPage, taxYearOpt, origin)
        auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(user, paymentAllocations))
        Ok(paymentAllocationView(paymentAllocations, backUrl = backUrl, user.saUtr,
          serviceNavigationPartial = user.serviceNavigationPartial,
          origin = origin, gatewayPage = sessionGatewayPage,
          creditsRefundsRepayEnabled = isEnabled(CreditsRefundsRepay))(implicitly, messages, user))

      case Left(PaymentAllocationError(Some(Http.Status.NOT_FOUND))) =>
        Redirect(redirectUrl)
      case _ => itvcErrorHandler.showInternalServerError()
    }
  }

  def viewPaymentAllocationAgent(documentNumber: String): Action[AnyContent] = {
    authActions.asMTDPrimaryAgent().async {
      implicit mtdItUser =>
        handleRequest(
          itvcErrorHandler = itvcErrorHandlerAgent,
          documentNumber = documentNumber,
          redirectUrl = redirectUrlAgent,
          isAgent = true
        )
    }
  }
}
