/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers.tfc

import calculators.TFCCalculator
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import controllers.FakeCCCalculatorApplication
import helper.JsonRequestHelper._
import models.input.APIModels.Request
import models.output.OutputAPIModel.AwardPeriod
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.AuditEvents
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

/**
 * Created by roma on 30/12/15.
 */
class TFCCalculatorControllerSpec extends UnitSpec with FakeCCCalculatorApplication with MockitoSugar {

  val mockTFCCalculatorController = new TFCCalculatorController with TFCCalculator {
    override val calculator = mock[TFCCalculatorService]
    override val auditEvent = mock[AuditEvents]
  }

  implicit val request = FakeRequest()

  "TFCCalculatorController" should {

    "not return NOT_FOUND (calculate) endpoint" in {
      val result = route(FakeRequest(POST, "/cc-calculator/tax-free-childcare/calculate"))
      result.isDefined shouldBe true
      status(result.get) should not be NOT_FOUND
    }

    "Return Bad Request with error message if a request for a different scheme is passed(e.g. TC) " in {
      val controller = mockTFCCalculatorController
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_12.json")
      val inputJson: JsValue = Json.parse(resource.toString)
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))

      val outputJSON = Json.parse(
        """
          |{
          |    "status": 400,
          |    "error": "You have provided a wrong type of request"
          |}
        """.stripMargin)

      status(result) shouldBe Status.BAD_REQUEST
      jsonBodyOf(result) shouldBe outputJSON
    }

    "Return Internal Server Error with error message if an exception is thrown during calculation " in {
      val controller = mockTFCCalculatorController
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      val resource: JsonNode = JsonLoader.fromResource("/json/tfc/input/calculator_input_test.json")
      val inputJson: JsValue = Json.parse(resource.toString)
      val JsonResult = inputJson.validate[Request]

      when(controller.calculator.award(mockEq(JsonResult.get))).thenReturn(Future.failed(new Exception("Something bad happened")))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      val outputJSON = Json.parse(
        """
          |{
          |    "status": 500,
          |    "error": "Something bad happened"
          |}
        """.stripMargin)

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe outputJSON
    }


    "Valid JSON at /tax-free-childcare/calculate" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/calculator_input_test.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.OK
    }

    "Accept invalid JSON at /tfc/calculate and return a BadRequest with an error (0 TFC Periods)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/no_period.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
          |{
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/periods",
          |         "validationErrors":[
          |            {
          |               "message":"Please provide at least 1 Period",
          |               "args":[
          |
          |               ]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Accept invalid JSON at /tax-free-childcare/calculate and return a BadRequest with an error (negative value in childcare cost)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/negative_childcareCost.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
          {
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/periods(0)/children(0)/childcareCost",
          |         "validationErrors":[
          |            {
          |               "message":"Childcare Spend cost should not be less than 0.00",
          |               "args":[
          |
          |               ]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Accept invalid JSON at /tfc/calculate and return a BadRequest with an error (no children)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/no_children.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
      {
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/periods(0)/children",
          |         "validationErrors":[
          |            {
          |               "message":"Please provide at least 1 child or maximun of 25 children",
          |               "args":[
          |
          |               ]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Accept invalid JSON at /tax-free-childcare/calculate and return a BadRequest with an error (date missing)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/date_missing.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
          |{
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/until",
          |         "validationErrors":[
          |            {
          |               "message":"error.path.missing",
          |               "args":[
          |
          |               ]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Accept invalid JSON at /tax-free-childcare/calculate and return a BadRequest with an error (child id negative)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/child_negative_id.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
          |{
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/periods(0)/children(0)/id",
          |         "validationErrors":[
          |            {
          |               "message":"ID should not be less than 0",
          |               "args":[
          |
          |               ]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Accept invalid JSON at /tax-free-childcare/calculate and return a BadRequest with an error (Invalid data type)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/invalid_data_type.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
          |{
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/until",
          |         "validationErrors":[
          |            {
          |               "message":"error.expected.jodadate.format",
          |               "args":["yyyy-MM-dd"]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Valid JSON at /tax-free-childcare/calculate(child from and until date is null)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/child_from_until_date_null.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.OK
    }

    "Valid JSON at /tax-free-childcare/calculate(childname is null)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/childName_none.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.OK
    }

    "Valid JSON at /tax-free-childcare/calculate(no disability field)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/no_disability.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.OK
    }

    "Accept invalid JSON at /tax-free-childcare/calculate and return a BadRequest with an error (max child name length > 25)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/childName_length_invalid.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")

      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST

      val outputJSON = Json.parse(
        """
          |{
          |   "status":400,
          |   "errors":[
          |      {
          |         "path":"/payload/eligibility/tfc/periods(0)/children(0)/name",
          |         "validationErrors":[
          |            {
          |               "message":"error.maxLength",
          |               "args":[25]
          |            }
          |         ]
          |      }
          |   ]
          |}
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJSON
    }

    "Valid JSON at /tax-free-childcare/calculate(TFC Award Calculation Wire up)" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/wire_up_flow_through.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.OK
    }

    "Accept invalid JSON at /tax-free-childcare/calculate(TFC Award Calculation Wire up - wrong scheme(esc))" in {
      val controller = mockTFCCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tfc/input/incorrect_scheme_name.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json")
      when(controller.calculator.award(any[Request]())).thenReturn(Future.successful(AwardPeriod()))
      val result = await(executeAction(controller.calculate(), request, inputJson.toString()))
      status(result) shouldBe Status.BAD_REQUEST
    }

  }
}