/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.esc

import calculators.ESCCalculator
import models.input.esc.ESCCalculatorInput
import models.output.esc.ESCCalculatorOutput
import play.api.Logger
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.Action
import service.AuditEvents
import play.api.i18n.{I18nSupport, MessagesApi}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.microservice.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ESCCalculatorController @Inject()(val messagesApi: MessagesApi) extends BaseController with I18nSupport {

  val auditEvent: AuditEvents = AuditEvents
  val calculator: ESCCalculator = ESCCalculator

  def calculate: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      request.body.validate[ESCCalculatorInput].fold(
        error => {
          Logger.warn("ESC Calculator Validation JsError in ESCCalculatorController.calculate")
          Future.successful(BadRequest(utils.JSONFactory.generateErrorJSON(play.api.http.Status.BAD_REQUEST, Left(error))))
        },
        result => {
          auditEvent.auditESCRequest(result.toString)
          println("----------------- elig: " + result)
          calculator.award(result).map {
            response =>
              val jsonResponse = Json.toJson[ESCCalculatorOutput](response)
              println("--------------- calc: " + jsonResponse)
              auditEvent.auditESCResponse(jsonResponse.toString())
              Ok(jsonResponse)
          } recover {
            case e: Exception =>
              Logger.warn(s"ESC Calculator Exception in ESCCalculatorController.calculate: ${e.getMessage}")
              InternalServerError(utils.JSONFactory.generateErrorJSON(play.api.http.Status.INTERNAL_SERVER_ERROR, Right(e)))
          }
        }
      )
  }
}
