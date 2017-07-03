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

package controllers.tc

import calculators.TCCalculator
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import models.input.tc.TCCalculatorInput
import models.output.tc.TCCalculation
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.mockito.Matchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.AuditEvents
import utils.FakeCCCalculatorApplication
import scala.concurrent.Future
import play.api.i18n.Messages.Implicits._

class TaxCreditCalculatorControllerSpec extends FakeCCCalculatorApplication with MockitoSugar {

  val mockTaxCreditCalculatorController = new TaxCreditCalculatorController(applicationMessagesApi) with TCCalculator {
    override val calculator =  mock[TCCalculatorService]
    override val auditEvent = mock[AuditEvents]
  }

  implicit val request = FakeRequest()

  "TaxCreditCalculatorController" should {

    "not return NOT_FOUND (income-advice) endpoint" in {
      val result = route(app, FakeRequest(POST, "/cc-calculator/tax-credits/calculate/income-advice"))
      result.isDefined shouldBe true
      status(result.get) should not be NOT_FOUND
    }

    "not return NOT_FOUND (total-award) endpoint" in {
      val result = route(app, FakeRequest(POST, "/cc-calculator/tax-credits/calculate/total-award"))
      result.isDefined shouldBe true
      status(result.get) should not be NOT_FOUND
    }

    "Accept valid JSON at /tax-credits/calculate/total-award" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.award(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(),
        LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.OK
    }

    "Accept valid JSON at /tax-credits/calculate/income-advice" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.incomeAdvice(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.incomeAdvice() (request))
      status(result) shouldBe Status.OK
    }

    "Accept invalid schema json and should return Bad request" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/no_payload.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.incomeAdvice(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.incomeAdvice() (request))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "Accept invalid json with incorrect data type json and return a Bad request" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/incorrect_data_type.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.award(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "Accept invalid json if houseHold Income is less than 0.00 and return Bad request" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/negative_household_income.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.incomeAdvice(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.incomeAdvice() (request))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "Accept a valid json if houseHold Income amount is 0.00 and return 200" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/no_household_income.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.incomeAdvice(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.incomeAdvice()(request))
      status(result) shouldBe Status.OK
    }

    "Accept a valid json if there is no childcare cost and return 200" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/no_childcare_cost.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.award(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.OK
    }

    "Accept invalid json if one of the childcare cost amounts is negative and return Bad Request" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/childcare_cost_two_children.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.award(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "Accept invalid json if both childcare cost amounts are negative and return a Bad Request" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson: JsValue  = Json.parse(JsonLoader.fromResource("/json/negative_childcare_cost_two_children.json").toString)
      val request: FakeRequest[JsValue] = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.award(any[TCCalculatorInput]())).thenReturn(Future.successful(TCCalculation(LocalDate.now(), LocalDate.now(), 0, 0, Nil)))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.BAD_REQUEST
    }

    "Accept a valid json object for scenario 1 for total award and return a valid response" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val award = TCCalculator.calculator.award(JsonResult.get)

      when(controller.calculator.award(mockEq(JsonResult.get))).thenReturn(Future.successful(award))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.OK

      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val firstPeriodTo = LocalDate.parse("2017-04-06", formatter)
      val outputJson = Json.parse(
        s"""
         |{
          |   "from": "${firstPeriodFrom.toString("yyyy-MM-dd")}",
          |   "until": "${firstPeriodTo.toString("yyyy-MM-dd")}",
          |   "totalAwardAmount": 2426.29,
          |   "houseHoldAdviceAmount": 0.00,
            | "taxYears": [
            |   {
            |      "from": "${firstPeriodFrom.toString("yyyy-MM-dd")}",
            |      "until": "${firstPeriodTo.toString("yyyy-MM-dd")}",
            |      "taxYearAwardAmount": 2426.29,
            |      "taxYearAdviceAmount": 0.00,
            |      "periods": [
              |     {
                |      "from": "${firstPeriodFrom.toString("yyyy-MM-dd")}",
                |      "until": "${firstPeriodTo.toString("yyyy-MM-dd")}",
                 |     "periodNetAmount": 2426.29,
               |       "periodAdviceAmount": 0.00,
               |       "elements": {
               |           "wtcWorkElement": {
                 |             "netAmount": 0.00,
                 |             "maximumAmount": 2078.08,
                 |             "taperAmount": 2078.08
               |           },
               |           "wtcChildcareElement": {
                 |             "maximumAmount": 878.41,
                 |             "netAmount": 686.28,
                 |             "taperAmount": 192.13
               |           },
               |           "ctcIndividualElement": {
                 |             "netAmount": 1455.42,
                 |             "maximumAmount": 1455.42,
                 |             "taperAmount": 0.00
               |           },
               |           "ctcFamilyElement": {
                 |             "netAmount": 284.59,
                 |             "maximumAmount": 284.59,
                 |             "taperAmount": 0.00
               |           }
             |          }
              |       }
               |     ]
             |     }
           |     ]
         | }
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJson
    }

    "Accept a valid json object for scenario 52 for income advice and return a valid response" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val award = TCCalculator.calculator.incomeAdvice(JsonResult.get)

      when(controller.calculator.incomeAdvice(mockEq(JsonResult.get))).thenReturn(Future.successful(award))
      val result = await(controller.incomeAdvice()(request))
      status(result) shouldBe Status.OK

      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val firstPeriodTo = LocalDate.parse("2016-12-12", formatter)
      val secondPeriodFrom = LocalDate.parse("2016-12-12", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      val outputJson = Json.parse(
        s"""
          |{
          | "from": "${firstPeriodFrom.toString("yyyy-MM-dd")}",
          | "until": "${secondPeriodTo.toString("yyyy-MM-dd")}",
          | "totalAwardAmount": 0.00,
          | "houseHoldAdviceAmount": 12055.6792,
          | "taxYears": [
          | {
          |   "from": "${firstPeriodFrom.toString("yyyy-MM-dd")}",
          |   "until": "${secondPeriodTo.toString("yyyy-MM-dd")}",
          |   "taxYearAwardAmount": 0.00,
          |   "taxYearAdviceAmount": 12055.6792,
          |   "periods": [
          |     {
          |      "from": "${firstPeriodFrom.toString("yyyy-MM-dd")}",
          |      "until": "${firstPeriodTo.toString("yyyy-MM-dd")}",
          |     "periodNetAmount": 0.00,
         |       "periodAdviceAmount": 7903.0752,
         |       "elements": {
         |           "wtcWorkElement": {
         |             "netAmount": 995.60,
         |             "maximumAmount": 995.60,
         |             "taperAmount": 0.00
         |           },
         |           "wtcChildcareElement": {
         |             "netAmount": 349.52,
         |             "maximumAmount": 349.52,
         |             "taperAmount": 0.00
         |           },
         |           "ctcIndividualElement": {
         |             "netAmount": 1232.72,
         |             "maximumAmount": 1232.72,
         |             "taperAmount": 0.00
         |           },
         |           "ctcFamilyElement": {
         |             "netAmount": 113.24,
         |             "maximumAmount": 113.24,
         |             "taperAmount": 0.00
         |           }
         |         }
         |     },
         |     {
         |       "from": "${secondPeriodFrom.toString("yyyy-MM-dd")}",
         |       "until": "${secondPeriodTo.toString("yyyy-MM-dd")}",
         |       "periodNetAmount": 0.00,
         |       "periodAdviceAmount": 4152.604,
         |       "elements": {
         |         "wtcWorkElement": {
         |             "netAmount": 872.85,
         |             "maximumAmount": 872.85,
         |             "taperAmount": 0.00
         |           },
         |           "wtcChildcareElement": {
         |             "netAmount": 0.00,
         |             "maximumAmount": 0.00,
         |             "taperAmount": 0.00
         |           },
         |           "ctcIndividualElement": {
         |             "netAmount": 0.00,
         |             "maximumAmount": 0.00,
         |             "taperAmount": 0.00
         |             },
         |           "ctcFamilyElement": {
         |             "netAmount": 0.00,
         |             "maximumAmount": 0.00,
         |             "taperAmount": 0.00
         |           }
         |         }
         |       }
          |     ]
          |     }
          |   ]
          | }
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputJson
    }

    "(Proratering) Accept a valid json object for scenario 61 for total award and return a valid response" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/scenario_61.json").toString)

      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val award = TCCalculator.calculator.award(JsonResult.get)

      when(controller.calculator.award(mockEq(JsonResult.get))).thenReturn(Future.successful(award))
      val result = await(controller.calculate()(request))
      status(result) shouldBe Status.OK

      val outputResult = Json.parse(
        s"""
        {
             "from": "2016-09-27",
             "until": "2017-04-06",
             "totalAwardAmount": 2982.17,
             "houseHoldAdviceAmount": 0.00,
             "taxYears": [
                 {
                     "from": "2016-09-27",
                     "until": "2017-04-06",
                     "taxYearAwardAmount": 2982.17,
                     "taxYearAdviceAmount": 0.00,
                      "periods": [
                       {
                        "from": "2016-09-27",
                        "until":"2016-12-12",
                         "periodNetAmount": 2982.17,
                         "periodAdviceAmount": 0.00,
                         "elements": {
                             "wtcWorkElement": {
                               "netAmount": 92.27,
                               "maximumAmount": 995.60,
                               "taperAmount": 903.33
                             },
                             "wtcChildcareElement": {
                               "netAmount": 699.58,
                               "maximumAmount": 699.58,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 2077.08,
                               "maximumAmount": 2077.08,
                               "taperAmount": 0.00
                             },
                             "ctcFamilyElement": {
                               "netAmount": 113.24,
                               "maximumAmount": 113.24,
                               "taperAmount": 0.00
                             }
                           }
                       },
                       {
                         "from": "2016-12-12",
                         "until": "2017-04-06",
                         "periodNetAmount": 0.00,
                         "periodAdviceAmount": 0.00,
                         "elements": {
                           "wtcWorkElement": {
                               "netAmount": 0.00,
                               "maximumAmount":  872.85,
                               "taperAmount":  872.85
                             },
                             "wtcChildcareElement": {
                               "netAmount": 0.00,
                               "maximumAmount": 0.00,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 0.00,
                               "maximumAmount": 0.00,
                               "taperAmount": 0.00
                               },
                             "ctcFamilyElement": {
                               "maximumAmount": 0.00,
                               "netAmount": 0.00,
                               "taperAmount": 0.00
                             }
                           }
                         }
                 ]
             }
             ]
           }
        """.stripMargin)

      jsonBodyOf(result) shouldBe outputResult
    }

    "(Proratering) Accept a valid json object for scenario 61 for total advice and return a valid response" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tc/input/2016/scenario_61.json").toString)

      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val incomeAdvice = TCCalculator.calculator.incomeAdvice(JsonResult.get)

      when(controller.calculator.incomeAdvice(mockEq(JsonResult.get))).thenReturn(Future.successful(incomeAdvice))
      val result = await(controller.incomeAdvice()(request))

      val outputResult = Json.parse(
        s"""
        {
             "from": "2016-09-27",
             "until": "2017-04-06",
             "totalAwardAmount": 0.00,
             "houseHoldAdviceAmount": 14970.064,
             "taxYears": [
                 {
                     "from": "2016-09-27",
                     "until": "2017-04-06",
                     "taxYearAwardAmount": 0.00,
                     "taxYearAdviceAmount": 14970.064,
                      "periods": [
                       {
                        "from": "2016-09-27",
                        "until":"2016-12-12",
                         "periodNetAmount": 0.00,
                         "periodAdviceAmount": 10817.46,
                         "elements": {
                             "wtcWorkElement": {
                               "netAmount": 995.60,
                               "maximumAmount": 995.60,
                               "taperAmount": 0.00
                             },
                             "wtcChildcareElement": {
                               "netAmount": 699.58,
                               "maximumAmount": 699.58,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 2077.08,
                               "maximumAmount": 2077.08,
                               "taperAmount": 0.00
                             },
                             "ctcFamilyElement": {
                               "netAmount": 113.24,
                               "maximumAmount": 113.24,
                               "taperAmount": 0.00
                             }
                           }
                       },
                       {
                         "from": "2016-12-12",
                         "until": "2017-04-06",
                         "periodNetAmount": 0.00,
                         "periodAdviceAmount": 4152.604,
                         "elements": {
                           "wtcWorkElement": {
                               "netAmount": 872.85,
                               "maximumAmount": 872.85,
                               "taperAmount": 0.00
                             },
                             "wtcChildcareElement": {
                               "netAmount": 0.00,
                               "maximumAmount": 0.00,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 0.00,
                               "maximumAmount": 0.00,
                               "taperAmount": 0.00
                               },
                             "ctcFamilyElement": {
                               "maximumAmount": 0.00,
                               "netAmount": 0.00,
                               "taperAmount": 0.00
                             }
                           }
                         }
                 ]
             }
             ]
           }
        """.stripMargin)

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe outputResult
    }

    "(Proratering) Accept a valid json object for scenario 5(2017 - 2018) for total advice and return a valid response" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/tc/input/2017/scenario_5.json").toString)

      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val incomeAdvice = TCCalculator.calculator.incomeAdvice(JsonResult.get)

      when(controller.calculator.incomeAdvice(mockEq(JsonResult.get))).thenReturn(Future.successful(incomeAdvice))
      val result = await(controller.incomeAdvice()(request))

      val outputResult = Json.parse(
        s"""
       {
             "from": "2016-09-27",
             "until": "2018-02-15",
             "totalAwardAmount": 0.00,
             "houseHoldAdviceAmount": 69256.9168,
             "taxYears": [
                 {
                     "from": "2016-09-27",
                     "until": "2017-04-06",
                     "taxYearAwardAmount": 0.00,
                     "taxYearAdviceAmount": 29664.5736,
                      "periods": [
                       {
                        "from": "2016-09-27",
                        "until":"2016-12-12",
                         "periodNetAmount": 0.0,
                         "periodAdviceAmount": 13425.1124,
                         "elements": {
                             "wtcWorkElement": {
                               "netAmount": 1879.48,
                               "maximumAmount": 1879.48,
                               "taperAmount": 0.00
                             },
                             "wtcChildcareElement": {
                               "netAmount": 1224.13,
                               "maximumAmount": 1224.13,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 1737.36,
                               "maximumAmount": 1737.36,
                               "taperAmount": 0.00
                             },
                             "ctcFamilyElement": {
                               "netAmount": 113.24,
                               "maximumAmount": 113.24,
                               "taperAmount": 0.00
                             }
                           }
                       },
                       {
                         "from": "2016-12-12",
                         "until": "2017-04-06",
                         "periodNetAmount": 0.00,
                         "periodAdviceAmount": 16239.4612,
                         "elements": {
                           "wtcWorkElement": {
                               "netAmount": 2843.95,
                               "maximumAmount": 2843.95,
                               "taperAmount": 0.00
                             },
                             "wtcChildcareElement": {
                               "netAmount": 1058.58,
                               "maximumAmount": 1058.58,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 1752.60,
                               "maximumAmount": 1752.60,
                               "taperAmount": 0.00
                               },
                             "ctcFamilyElement": {
                               "maximumAmount": 171.35,
                               "netAmount": 171.35,
                               "taperAmount": 0.00
                             }
                           }
                         }
                 ]
             },
                 {
                     "from": "2017-04-06",
                     "until": "2018-02-15",
                     "taxYearAwardAmount": 0.00,
                     "taxYearAdviceAmount": 39592.3432,
                      "periods": [
                       {
                        "from": "2017-04-06",
                        "until":"2017-09-01",
                         "periodNetAmount": 0.00,
                         "periodAdviceAmount": 20942.7992,
                         "elements": {
                             "wtcWorkElement": {
                               "netAmount": 3677.8,
                               "maximumAmount": 3677.8,
                               "taperAmount": 0.00
                             },
                             "wtcChildcareElement": {
                               "netAmount": 1362.34,
                               "maximumAmount": 1362.34,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 2255.52,
                               "maximumAmount": 2255.52,
                               "taperAmount": 0.00
                             },
                             "ctcFamilyElement": {
                               "netAmount": 220.52,
                               "maximumAmount": 220.52,
                               "taperAmount": 0.00
                             }
                           }
                       },
                       {
                         "from": "2017-09-01",
                         "until": "2018-02-15",
                         "periodNetAmount": 0.00,
                         "periodAdviceAmount": 18649.544,
                         "elements": {
                           "wtcWorkElement": {
                               "netAmount": 4149.95,
                               "maximumAmount": 4149.95,
                               "taperAmount": 0.00
                             },
                             "wtcChildcareElement": {
                               "netAmount": 768.03,
                               "maximumAmount": 768.03,
                               "taperAmount": 0.00
                             },
                             "ctcIndividualElement": {
                               "netAmount": 1272.54,
                               "maximumAmount": 1272.54,
                               "taperAmount": 0.00
                               },
                             "ctcFamilyElement": {
                                "netAmount": 248.83,
                               "maximumAmount": 248.83,
                               "taperAmount": 0.00
                             }
                           }
                         }
                 ]
             }
             ]
           }
        """.stripMargin)

      status(result) shouldBe Status.OK
      jsonBodyOf(result) shouldBe outputResult
    }


    "Accept invalid json if childcare spend is less than 0.00 and return a valid response" in {
      val controller = mockTaxCreditCalculatorController
      val inputJson = Json.parse(JsonLoader.fromResource("/json/negative_childcare_cost.json").toString)
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)
      val result = await(controller.calculate()(request))

      val outputJSON = Json.parse(
        """
          |{
          |    "status": 400,
          |    "errors":
          |     [
          |        {
          |        "path": "/taxYears(0)/periods(0)/children(0)/childcareCost",
          |         "validationErrors":
          |         [
          |            {
          |                "message": "Childcare Spend cost should not be less than 0.00",
          |                "args": []
          |            }
          |         ]
          |       }
          |    ]
          |}
        """.stripMargin)

      status(result) shouldBe Status.BAD_REQUEST
      jsonBodyOf(result) shouldBe outputJSON
    }

    "Return Internal Server Error with error message if an exception is thrown during income advice calculation " in {
      val controller = mockTaxCreditCalculatorController
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_23.json")
      val inputJson: JsValue = Json.parse(resource.toString)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.incomeAdvice(mockEq(JsonResult.get))).thenReturn(Future.failed(new Exception("Something bad happened")))
      val result = await(controller.incomeAdvice()(request))
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

    "Return Internal Server Error with error message if an exception is thrown during total reward calculation " in {
      val controller = mockTaxCreditCalculatorController
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json")
      val inputJson: JsValue = Json.parse(resource.toString)
      val JsonResult = inputJson.validate[TCCalculatorInput]
      val request = FakeRequest("POST", "").withHeaders("Content-Type" -> "application/json").withBody(inputJson)

      when(controller.calculator.award(mockEq(JsonResult.get))).thenReturn(Future.failed(new Exception("Something bad happened")))
      val result = await(controller.calculate()(request))
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

  }

}
