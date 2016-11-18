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

package calculators

import calculators.TCCalculator.TCCalculatorService
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import controllers.FakeCCCalculatorApplication
import models.input.APIModels._
import models.input.tc._
import models.output.OutputAPIModel.AwardPeriod
import models.output.tc.{Element, Elements}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{CCJsonLogger, Periods, TCConfig}

import scala.collection.immutable.Nil
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by adamconder on 08/06/15.
 */

class TCCalculatorSpec extends UnitSpec with FakeCCCalculatorApplication with CCJsonLogger with org.scalatest.PrivateMethodTester {

  "TCCalculator" should {

    "round up for double numbers if less than 5" in {
      val cost: BigDecimal = 12.241212342
      val result: BigDecimal = TCCalculator.calculator.round(cost)
      result shouldBe 12.25
    }

    "round up for double numbers if less than 5 (.00001)" in {
      val cost: BigDecimal = 12.00001
      val result: BigDecimal = TCCalculator.calculator.round(cost)
      result shouldBe 12.01
    }

    "round up for double numbers if more than 5 (.945)" in {
      val cost: BigDecimal = 12.9599231531231
      val result: BigDecimal = TCCalculator.calculator.round(cost)
      result shouldBe 12.96
    }

    "round up for double numbers if less than 5 (.4444)" in {
      val cost: BigDecimal = 12.44000000000001
      val result: BigDecimal = TCCalculator.calculator.round(cost)
      result shouldBe 12.45
    }

    "not round up for double numbers if all digits after decimal point are 0" in {
      val cost: BigDecimal = 12.9800
      val result: BigDecimal = TCCalculator.calculator.round(cost)
      result shouldBe 12.98
    }

    "round up to pound for double numbers if less than 5 (.241212342)" in {
      val cost: BigDecimal = 12.241212342
      val result: BigDecimal = TCCalculator.calculator.roundToPound(cost)
      result shouldBe 13
    }

    "round up to pound for double numbers if less than 5 (.01)" in {
      val cost: BigDecimal = 12.01
      val result: BigDecimal = TCCalculator.calculator.roundToPound(cost)
      result shouldBe 13
    }

    "round up to pound for double numbers if more than 5 (.51)" in {
      val cost: BigDecimal = 12.51
      val result: BigDecimal = TCCalculator.calculator.roundToPound(cost)
      result shouldBe 13
    }

    "round up to pound for double numbers if more than 5 (.99)" in {
      val cost: BigDecimal = 12.99
      val result: BigDecimal = TCCalculator.calculator.roundToPound(cost)
      result shouldBe 13
    }

    "not round up to pound for double numbers if all digits after decimal point are 0" in {
      val cost: BigDecimal = 12.00
      val result: BigDecimal = TCCalculator.calculator.roundToPound(cost)
      result shouldBe 12
    }

    "truncate the number to three decimal places(.1192)" in {
      val cost: BigDecimal = 12.1192
      val result: BigDecimal = TCCalculator.calculator.roundDownToThreeDigits(cost)
      result shouldBe 12.119
    }

    "truncate the number to three decimal places(.1145)" in {
      val cost: BigDecimal = 12.1145
      val result: BigDecimal = TCCalculator.calculator.roundDownToThreeDigits(cost)
      result shouldBe 12.114
    }

    "truncate the number to three decimal places(.111001)" in {
      val cost: BigDecimal = 12.111001
      val result: BigDecimal = TCCalculator.calculator.roundDownToThreeDigits(cost)
      result shouldBe 12.111
    }

    "truncate the number to three decimal places(.0009)" in {
      val cost: BigDecimal = 12.0009
      val result: BigDecimal = TCCalculator.calculator.roundDownToThreeDigits(cost)
      result shouldBe 12.000
    }

    "truncate the number to three decimal places(.465753)" in {
      val cost: BigDecimal = 142.465753
      val result: BigDecimal = TCCalculator.calculator.roundDownToThreeDigits(cost)
      result shouldBe 142.465
    }

    "pro-tata an amount of money between two dates (after is before until date)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-04-2016", formatter)
      //String representation in order to test the whole value
      val result = TCCalculator.calculator.amountForDateRange(BigDecimal(0.00), Periods.Weekly, fromDate, toDate, rounded = false, truncated = false)
      result shouldBe BigDecimal(0.00)
    }

    "(weekly) pro-rata an amount of money between two dates (not rounded and not truncated)" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      //String representation in order to test the whole value
      val result: String = TCCalculator.calculator.amountForDateRange(cost, Periods.Weekly, fromDate, toDate, rounded = false, truncated = false).toString()
      result shouldBe "2849.315068493150684931506849315068"
    }

    "(Fortnightly) pro-rata an amount of money between two dates (not rounded and not truncated)" in {
      val cost: BigDecimal = 333.34
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      //String representation in order to test the whole value
      val result: String = TCCalculator.calculator.amountForDateRange(cost, Periods.Fortnightly, fromDate, toDate, rounded = false, truncated = false).toString()
      result shouldBe "474.8953424657534246575342465753424"
    }

    "(Monthly) pro-rata an amount of money between two dates (not rounded and not truncated)" in {
      val cost: BigDecimal = 100.99
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      //String representation in order to test the whole value
      val result: String = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate, rounded = false, truncated = false).toString()
      result shouldBe "66.40438356164383561643835616438356"
    }

    "(Quarterly) pro-rata an amount of money between two dates (not rounded and not truncated)" in {
      val cost: BigDecimal = 2000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      //String representation in order to test the whole value
      val result: String = TCCalculator.calculator.amountForDateRange(cost, Periods.Quarterly, fromDate, toDate, rounded = false, truncated = false).toString()
      result shouldBe "438.3561643835616438356164383561644"
    }

    "(Yearly) pro-rata an amount of money between two dates (not rounded and not truncated)" in {
      val cost: BigDecimal = 9999.01
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      //String representation in order to test the whole value
      val result: String = TCCalculator.calculator.amountForDateRange(cost, Periods.Yearly, fromDate, toDate, rounded = false, truncated = false).toString()
      result shouldBe "547.8909589041095890410958904109590"
    }

    "(weekly) pro-rata an amount of money between two dates (not rounded and truncated)" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Weekly, fromDate, toDate, rounded = false, truncated = true)
      result shouldBe 2849.300
    }

    "(fortnightly) pro-rata an amount of money between two dates (not rounded and truncated)" in {
      val cost: BigDecimal = 333.34
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Fortnightly, fromDate, toDate, rounded = false, truncated = true)
      result shouldBe 474.880
    }

    "(monthly) pro-rata an amount of money between two dates (not rounded and truncated)" in {
      val cost: BigDecimal = 100.99
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate, rounded = false, truncated = true)
      result shouldBe 66.400
    }

    "(quarterly) pro-rata an amount of money between two dates (not rounded and truncated)" in {
      val cost: BigDecimal = 2000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Quarterly, fromDate, toDate, rounded = false, truncated = true)
      result shouldBe 438.340
    }

    "(yearly) pro-rata an amount of money between two dates (not rounded and truncated)" in {
      val cost: BigDecimal = 9999.01
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Yearly, fromDate, toDate, rounded = false, truncated = true)
      result shouldBe 547.880
    }

    "(weekly) pro-rata an amount of money between two dates (rounded and truncated)" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Weekly, fromDate, toDate, rounded = true, truncated = true)
      result shouldBe 2849.40
    }

    "(fortnightly) pro-rata an amount of money between two dates (rounded and truncated)" in {
      val cost: BigDecimal = 333.34
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Fortnightly, fromDate, toDate, rounded = true, truncated = true)
      result shouldBe 475.00
    }

    "(monthly) pro-rota an amount of money between two dates (rounded and truncated)" in {
      val cost: BigDecimal = 100.99
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate, rounded = true, truncated = true)
      result shouldBe 66.40
    }

    "(quarterly) pro-rata an amount of money between two dates (rounded and truncated)" in {
      val cost: BigDecimal = 2000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Quarterly, fromDate, toDate, rounded = true, truncated = true)
      result shouldBe 438.40
    }

    "(yearly) pro-rata an amount of money between two dates (rounded and truncated)" in {
      val cost: BigDecimal = 9999.01
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Yearly, fromDate, toDate, rounded = true, truncated = true)
      result shouldBe 548.00
    }

    "(weekly) pro-rata an amount of money between two dates (rounded and not truncated)" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Weekly, fromDate, toDate, rounded = true, truncated = false)
      result shouldBe 2849.40
    }

    "(fortnightly) pro-rata an amount of money between two dates (rounded and not truncated)" in {
      val cost: BigDecimal = 333.34
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Fortnightly, fromDate, toDate, rounded = true, truncated = false)
      result shouldBe 475.00
    }

    "(monthly) pro-rata an amount of money between two dates (rounded and not truncated)" in {
      val cost: BigDecimal = 100.99
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate, rounded = true, truncated = false)
      result shouldBe 66.60
    }

    "(quarterly) pro-rata an amount of money between two dates (rounded and not truncated)" in {
      val cost: BigDecimal = 2000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Quarterly, fromDate, toDate, rounded = true, truncated = false)
      result shouldBe 438.40
    }

    "(yearly) pro-rata an amount of money between two dates (rounded and not truncated)" in {
      val cost: BigDecimal = 9999.01
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Yearly, fromDate, toDate, rounded = true, truncated = false)
      result shouldBe 548.00
    }

    "(Fortnightly) pro-rata an amount of money between two dates" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Fortnightly, fromDate, toDate)
      result shouldBe 1424.80
    }

    "(monthly) pro-rata an amount of money between two dates" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate)
      result shouldBe 657.60
    }

    "(quarterly) pro-rata an amount of money between two dates" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Quarterly, fromDate, toDate)
      result shouldBe 219.20
    }

    "(yearly) pro-rata an amount of money between two dates" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Yearly, fromDate, toDate)
      result shouldBe 54.80
    }

    "(monthly) pro-rata an amount of money between two dates spanning two years" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("12-12-2016", formatter)
      val toDate = LocalDate.parse("06-04-2017", formatter)
      val result: BigDecimal = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate)
      result shouldBe 3781.20
    }

    "(monthly) pro-rata an amount of money between two dates spanning two years (truncated, but not rounded)" in {
      val cost: BigDecimal = 1000.00
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("12-12-2016", formatter)
      val toDate = LocalDate.parse("06-04-2017", formatter)
      //String representation in order to test the whole value
      val result: String = TCCalculator.calculator.amountForDateRange(cost, Periods.Monthly, fromDate, toDate, rounded = false).toString()
      result shouldBe "3780.740"
    }

    "convert the daily amount to a period (weekly) (Leap year - 366 days)" in {
      val cost : BigDecimal = 1000.00 // per quarter
      val result : BigDecimal = TCCalculator.calculator.amountFromPeriodToDaily(cost, Periods.Weekly, 366)
      TestCalculator.calculator.convertToCurrency(result) shouldBe "£142.08"
    }

    "convert the daily amount to a period (fortnightly) (Leap year - 366 days)" in {
      val cost : BigDecimal = 1000.00 // per quarter
      val result : BigDecimal = TCCalculator.calculator.amountFromPeriodToDaily(cost, Periods.Fortnightly, 366)
      TestCalculator.calculator.convertToCurrency(result) shouldBe "£71.04"
    }

    "convert the daily amount to a period (monthly) (Leap year - 366 days)" in {
      val cost : BigDecimal = 1000.00 // per quarter
      val result : BigDecimal = TCCalculator.calculator.amountFromPeriodToDaily(cost, Periods.Monthly, 366)
      TestCalculator.calculator.convertToCurrency(result) shouldBe "£32.79"
    }

    "convert the daily amount to a period (quarterly) (Leap year - 366 days)" in {
      val cost : BigDecimal = 1000.00 // per quarter
      val result : BigDecimal = TCCalculator.calculator.amountFromPeriodToDaily(cost, Periods.Quarterly, 366)
      TestCalculator.calculator.convertToCurrency(result) shouldBe "£10.93"
    }

    "convert the daily amount to a period (yearly) (Leap year - 366 days)" in {
      val cost : BigDecimal = 1000.00 // per quarter
      val result : BigDecimal = TCCalculator.calculator.amountFromPeriodToDaily(cost, Periods.Yearly, 366)
      TestCalculator.calculator.convertToCurrency(result) shouldBe "£2.73"
    }

    "convert the daily amount to a period (Invalid) (Leap year - 366 days)" in {
      val cost : BigDecimal = 1000.00 // per quarter
      val result : BigDecimal = TCCalculator.calculator.amountFromPeriodToDaily(cost, Periods.INVALID, 366)
      TestCalculator.calculator.convertToCurrency(result) shouldBe "£0.00"
    }

    "Determine earnings amount per period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-09-27", formatter)
      val untilDate = LocalDate.parse("2017-04-06", formatter)
      val period = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val income = BigDecimal(17000)
      val thresholdIncome = BigDecimal(4000)
      val result = TCCalculator.calculator.earningsAmountToTaperForPeriod(income, thresholdIncome, period)
      result shouldBe BigDecimal(5330.00)
    }

    "Determine net amount per element per period (taper amount is larger than element's max amount)" in {
      val taperAmount = BigDecimal(17000)
      val maximumAmount = BigDecimal(4000)
      val result = TCCalculator.calculator.netAmountPerElementPerPeriod(taperAmount, maximumAmount)
      result shouldBe BigDecimal(0.00)
    }

    "Determine net amount per element per period (taper amount is lower than element's max amount)" in {
      val taperAmount = BigDecimal(200)
      val maximumAmount = BigDecimal(17000)
      val result = TCCalculator.calculator.netAmountPerElementPerPeriod(taperAmount, maximumAmount)
      result shouldBe BigDecimal(16800.00)
    }

    "Determine percentage of an amount" in {
      val amount = BigDecimal(10000.00)
      val percentage = 10
      val result = TCCalculator.calculator.getPercentOfAmount(amount, percentage)
      result shouldBe BigDecimal(1000.00)
    }

    "Return an instance of TCCalculator" in {
      val calc = TCCalculator
      calc.isInstanceOf[TCCalculator] shouldBe true
    }

    "Return an instance of TCCalculatorService" in {
      val calc = TCCalculator
      calc.calculator.isInstanceOf[TCCalculatorService] shouldBe true
    }

    "Determine if wtc Work element net due amount is nil (amount is 0)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("12-12-2016", formatter)
      val toDate = LocalDate.parse("06-04-2017", formatter)
      val period = models.output.tc.Period(
        from = fromDate,
        until = toDate,
        elements = Elements(
          wtcWorkElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          wtcChildcareElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          ctcIndividualElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          ctcFamilyElement = Element(
            maximumAmount = BigDecimal(100.00)
          )
        )
      )
      val result = period.elements.wtcWorkElementNetDueIsNil
      result shouldBe true
    }

    "Determine if wtc Work element net due amount is nil (amount is not 0)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("12-12-2016", formatter)
      val toDate = LocalDate.parse("06-04-2017", formatter)
      val period = models.output.tc.Period(
        from = fromDate,
        until = toDate,
        elements = Elements(
          wtcWorkElement = Element(
            maximumAmount = BigDecimal(100.00),
            netAmount = BigDecimal(100.00)
          ),
          wtcChildcareElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          ctcIndividualElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          ctcFamilyElement = Element(
            maximumAmount = BigDecimal(100.00)
          )
        )
      )
      val result = period.elements.wtcWorkElementNetDueIsNil
      result shouldBe false
    }

    "(qualifying) determine if get basic element and the amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val basicElement = TCCalculator.calculator.basicElementForPeriod(period.head)
          basicElement._1 shouldBe true
          basicElement._2 shouldBe BigDecimal(1025.67)
        case JsError(e) => throw new RuntimeException(e.toList.toString())
      }
    }

    "(non qualifying) determine if get basic element and the amount for the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-09-27", formatter)
      val toDate = LocalDate.parse("2017-04-06", formatter)
      val period = models.input.tc.Period(
        from = fromDate,
        until = toDate,
        householdElements = HouseHoldElements(
          basic = false,
          hours30 = false,
          childcare = false,
          loneParent = false,
          secondParent = false,
          family = false
        ),
        claimants = List(),
        children = List()
      )
      val getsBasicElement = TCCalculator.calculator.basicElementForPeriod(period)
      getsBasicElement._1 shouldBe false
      getsBasicElement._2 shouldBe BigDecimal(0.00)
    }

    "determine if get 30 hours element and the amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_7.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val hours30Element = TCCalculator.calculator.hours30ElementForPeriod(period.head)
          hours30Element._1 shouldBe true
          hours30Element._2 shouldBe BigDecimal(424.02)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "determine if get disabled worker element and the amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_13.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val claimant = period.head.claimants.head
          val workerDisabiltyElement = TCCalculator.calculator.disabledWorkerElementForPeriod(period.head, claimant)
          workerDisabiltyElement._1 shouldBe true
          workerDisabiltyElement._2 shouldBe BigDecimal(1554.74)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "determine if get severely disabled worker element and the amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_19.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val claimant = period.head.claimants.head
          val severelyDisabledWorkerElement = TCCalculator.calculator.severelyDisabledWorkerElementForPeriod(period.head, claimant)
          severelyDisabledWorkerElement._1 shouldBe true
          severelyDisabledWorkerElement._2 shouldBe BigDecimal(668.50)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "determine if get lone parent element and amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val loneParentElement = TCCalculator.calculator.loneParentElementForPeriod(period.head)
          loneParentElement._1 shouldBe true
          loneParentElement._2 shouldBe BigDecimal(1052.41)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "determine if get second adult element and amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_18.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val coupleElement = TCCalculator.calculator.secondAdultElementForPeriod(period.head)
          coupleElement._1 shouldBe true
          coupleElement._2 shouldBe BigDecimal(1052.41)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(qualifying) determine if get family element for period and amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val familyElement = TCCalculator.calculator.maxFamilyElementForPeriod(period.head)
          familyElement._1 shouldBe true
          familyElement._2 shouldBe BigDecimal(286.50)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(non - qualifying) determine if get family element for period and amount for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_51.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // first period
          val period = x.payload.eligibility.tc.get.taxYears.head.periods.tail.head
          val familyElement = TCCalculator.calculator.maxFamilyElementForPeriod(period)
          familyElement._1 shouldBe false
          familyElement._2 shouldBe BigDecimal(0.00)

        // second period
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(qualifying) Determine if child gets the child basic element for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val childElement = TCCalculator.calculator.childOrYoungAdultBasicElementForPeriod(period.head, period.head.children.head)
          childElement._1 shouldBe true
          childElement._2 shouldBe BigDecimal(1455.42)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(non-qualifying) Determine if child gets the child basic element for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_51.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods.tail.head
          val childElement = TCCalculator.calculator.childOrYoungAdultBasicElementForPeriod(period, period.children.head)
          childElement._1 shouldBe false
          childElement._2 shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(qualifying) Determine if child gets disability element for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_11.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val childDisabiltyElement = TCCalculator.calculator.childOrYoungAdultDisabilityElementForPeriod(period.head, period.head.children.head)
          childDisabiltyElement._1 shouldBe true
          childDisabiltyElement._2 shouldBe BigDecimal(1644.51)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(non - qualifying) Determine if child gets disability element for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val childDisabiltyElement = TCCalculator.calculator.childOrYoungAdultDisabilityElementForPeriod(period.head, period.head.children.head)
          childDisabiltyElement._1 shouldBe false
          childDisabiltyElement._2 shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(qualifying) Determine if child gets severe disability element for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_11.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val childSevereDisabiltyElement = TCCalculator.calculator.childOrYoungAdultSevereDisabilityElementForPeriod(period.head, period.head.children.head)
          childSevereDisabiltyElement._1 shouldBe true
          childSevereDisabiltyElement._2 shouldBe BigDecimal(668.50)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(non-qualifying) Determine if child gets severe disability element for the period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_21.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val childSevereDisabiltyElement = TCCalculator.calculator.childOrYoungAdultSevereDisabilityElementForPeriod(period.head, period.head.children.head)
          childSevereDisabiltyElement._1 shouldBe false
          childSevereDisabiltyElement._2 shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(qualifying) determine child element(s) (as a total) for multiple children" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_44.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val childElement = TCCalculator.calculator.maxChildElementForPeriod(period.head)
          childElement._1 shouldBe true
          childElement._2 shouldBe BigDecimal(8323.78)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(one child qualifying, one not qualifying)(child + child) determine child element(s) (as a total) for multiple children" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // first period
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val period1ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.head)
          period1ChildElement._1 shouldBe true
          period1ChildElement._2 shouldBe BigDecimal(1233.48)

          // second period
          val period2ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.tail.head)
          period2ChildElement._1 shouldBe false
          period2ChildElement._2 shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(qualifying)(young adult + child) determine child element(s) (as a total) for multiple children" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_50.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // first period
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val period1ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.head)
          period1ChildElement._1 shouldBe true
          period1ChildElement._2 shouldBe BigDecimal(1158.24)

          // second period
          val period2ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.tail.head)
          period2ChildElement._1 shouldBe true
          period2ChildElement._2 shouldBe BigDecimal(876.30)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(no children) return BigDecimal(0.00) when checking weekly threshold spend for children" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val decoratedChildCareThreshold = PrivateMethod[BigDecimal]('getChildcareThresholdPerWeek)
      val result = TCCalculator.calculator invokePrivate decoratedChildCareThreshold(inputPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "(1 child) return BigDecimal(0.00) when checking weekly threshold spend for children" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val child = Child(id = 0, name = "Child 1", childcareCost = BigDecimal(2000.00), childcareCostPeriod = Periods.Monthly, childElements = ChildElements())

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List(child))

      val decoratedChildCareThreshold = PrivateMethod[BigDecimal]('getChildcareThresholdPerWeek)
      val result = TCCalculator.calculator invokePrivate decoratedChildCareThreshold(inputPeriod)
      result shouldBe BigDecimal(175.00)
    }

    "(5 children) return BigDecimal(0.00) when checking weekly threshold spend for children" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val child1 = Child(id = 0, name = "Child 1", childcareCost = BigDecimal(500.00), childcareCostPeriod = Periods.Monthly, childElements = ChildElements())
      val child2 = Child(id = 0, name = "Child 1", childcareCost = BigDecimal(2000.00), childcareCostPeriod = Periods.Monthly, childElements = ChildElements())
      val child3 = Child(id = 0, name = "Child 1", childcareCost = BigDecimal(300.00), childcareCostPeriod = Periods.Monthly, childElements = ChildElements())
      val child4 = Child(id = 0, name = "Child 1", childcareCost = BigDecimal(200.00), childcareCostPeriod = Periods.Monthly, childElements = ChildElements())
      val child5 = Child(id = 0, name = "Child 1", childcareCost = BigDecimal(100.00), childcareCostPeriod = Periods.Monthly, childElements = ChildElements())

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List(child1, child2, child3, child4, child5))

      val decoratedChildCareThreshold = PrivateMethod[BigDecimal]('getChildcareThresholdPerWeek)
      val result = TCCalculator.calculator invokePrivate decoratedChildCareThreshold(inputPeriod)
      result shouldBe BigDecimal(300.00)
    }

    "(all not qualifying) determine child element(s) (as a total) for multiple children" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // first period
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val period1ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.head)
          period1ChildElement._1 shouldBe true
          period1ChildElement._2 shouldBe BigDecimal(1233.48)
          // second period
          val period2ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.tail.head)
          period2ChildElement._1 shouldBe false
          period2ChildElement._2 shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(no children) determine the child element(s) (as a total) for no children" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_57.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // first period
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val period1ChildElement = TCCalculator.calculator.maxChildElementForPeriod(period.head)
          period1ChildElement._1 shouldBe false
          period1ChildElement._2 shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(claimant with partner both qualifying) determine wtc work element(s) (as a total) for multiple claimants" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_18.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val workElement = TCCalculator.calculator.maxWorkElementForPeriod(period.head)
          workElement._1 shouldBe true
          workElement._2 shouldBe BigDecimal(5187.56)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(claimant qualifying without partner) determine wtc work element (as a total) for single claimant" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_27.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val workElement = TCCalculator.calculator.maxWorkElementForPeriod(period.head)
          workElement._1 shouldBe true
          workElement._2 shouldBe BigDecimal(4056.84)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(claimant with partner both qualifying) determine wtc work element(s) (as a total) for multiple claimants with severe disability" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_32.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val workElement = TCCalculator.calculator.maxWorkElementForPeriod(period.head)
          workElement._1 shouldBe true
          workElement._2 shouldBe BigDecimal(6948.58)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(claimant without partner without children) determine wtc work element(s) (as a total) for single claimant (Not applicable for our journey)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_54.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val workElement = TCCalculator.calculator.maxWorkElementForPeriod(period.head)
          workElement._1 shouldBe false
          workElement._2 shouldBe BigDecimal(1025.67)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(claimant with partner without children) determine wtc work element(s) (as a total) for multiple claimants (Not applicable for our journey)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_55.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val workElement = TCCalculator.calculator.maxWorkElementForPeriod(period.head)
          workElement._1 shouldBe false
          workElement._2 shouldBe BigDecimal(2580.41)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there is only one child (not exceeding the element limit)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(895.24)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there is only one child (exceeding the element limit)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_2.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(3333.35)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there are 2 children (not exceeding childcare element limit)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_39.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(5695.26)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there are 2 children (edge case -> 300p/w)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_38.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(5714.31)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there are 2 children (exceeding childcare element limit)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_37.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(5714.31)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there are 3 children (not exceeding the element limit)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_43.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(2647.63)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine WTC childcare element when there are 3 children (exceeding the element limit)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_44.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val wtcChildcareElement = TCCalculator.calculator.maxChildcareElementForPeriod(period.head)
          wtcChildcareElement._1 shouldBe true
          wtcChildcareElement._2 shouldBe BigDecimal(5714.31)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine award period start and end dates when there is only one period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-09-27", formatter)
      val toDate = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val award = TCCalculator.calculator.award(x)
          award.tc.get.from shouldBe fromDate
          award.tc.get.until shouldBe toDate
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine award period start and end dates when there is more than one period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-09-27", formatter)
      val toDate = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val award = TCCalculator.calculator.award(x)
          award.tc.get.from shouldBe fromDate
          award.tc.get.until shouldBe toDate
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(2016/2017) Determine wtc income threshold for a period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val threshold = TCCalculator.calculator.wtcIncomeThresholdForPeriod(period.head)
          threshold shouldBe BigDecimal(3359.69)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(2016/2017) Determine ctc income threshold for a period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val threshold = TCCalculator.calculator.ctcIncomeThresholdForPeriod(period.head)
          threshold shouldBe BigDecimal(8428.83)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(2017/2018) Determine wtc income threshold for a period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2017/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val threshold = TCCalculator.calculator.wtcIncomeThresholdForPeriod(period.head)
          threshold shouldBe BigDecimal(3359.69)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(2017/2018) Determine ctc income threshold for a period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2017/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val threshold = TCCalculator.calculator.ctcIncomeThresholdForPeriod(period.head)
          threshold shouldBe BigDecimal(8428.83)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine single claimant income for a period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val threshold = TCCalculator.calculator.incomeForPeriod(x.payload.eligibility.tc.get.taxYears.head.houseHoldIncome, period.head)
          threshold shouldBe BigDecimal(8896.78)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine multiple claimant income for a period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_32.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val period = x.payload.eligibility.tc.get.taxYears.head.periods
          val threshold = TCCalculator.calculator.incomeForPeriod(x.payload.eligibility.tc.get.taxYears.head.houseHoldIncome, period.head)
          threshold shouldBe BigDecimal(17791.65)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(requires tapering) Determine if tapering of elements is required" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-12-2016", formatter)
      val threshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val income = BigDecimal(6420.01)
      val result = TCCalculator.calculator.isTaperingRequiredForElements(income, threshold)
      result shouldBe true
    }

    "(does not require tapering - income same as threshold) Determine if tapering of elements is required" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-12-2016", formatter)
      val threshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val income = BigDecimal(6420.00)
      val result = TCCalculator.calculator.isTaperingRequiredForElements(income, threshold)
      result shouldBe false
    }

    "(does not require tapering - income less) Determine if tapering of elements is required" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-12-2016", formatter)
      val threshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val income = BigDecimal(200.00)
      val result = TCCalculator.calculator.isTaperingRequiredForElements(income, threshold)
      result shouldBe false
    }

    "Determine which amount is higher (one amount is higher)" in {
      val amount = BigDecimal(100.00)
      val higherAmount = BigDecimal(100.0001)
      val result = TCCalculator.calculator.getHigherAmount(amount, higherAmount)
      result shouldBe higherAmount
    }

    "Determine which amount is higher (both amounts are the same)" in {
      val amount = BigDecimal(100.00)
      val higherAmount = BigDecimal(100.00)
      val result = TCCalculator.calculator.getHigherAmount(amount, higherAmount)
      result shouldBe higherAmount
      higherAmount shouldBe amount
    }

    "(does not require tapering - income less than threshold) Determine the net amounts of the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 5000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(4737.70)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(748.80)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(5504.20)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(547.50)
    }

    "(tapers all amounts fully) Determine the net amounts of the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 100000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(0.00)
    }

    "(no tapering required, single claimant claiming social security benefit) Determine the net amounts of a the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 100000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val claimantClaimingSocialSecurityBenefit = models.input.tc.Claimant(qualifying = true, doesNotTaper = true, isPartner = false, claimantElements = ClaimantDisability(), failures = Some(List()))

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(claimantClaimingSocialSecurityBenefit), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(4737.70)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(748.80)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(5504.20)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(547.50)
    }

    "(tapering required, single claimant not claiming social security benefit) Determine the net amounts of a the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 100000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val claimantClaimingSocialSecurityBenefit = models.input.tc.Claimant(qualifying = true, doesNotTaper = false, isPartner = false, claimantElements = ClaimantDisability(), failures = Some(List()))

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(claimantClaimingSocialSecurityBenefit), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(0.00)
    }

    "(no tapering required, joint claimants, one claimant claiming social security benefit) Determine the net amounts of a the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 100000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val claimant = models.input.tc.Claimant(qualifying = true, isPartner = false, claimantElements = ClaimantDisability(), failures = Some(List()))
      val claimantClaimingSocialSecurityBenefit = models.input.tc.Claimant(qualifying = true, doesNotTaper = true, isPartner = true, claimantElements = ClaimantDisability(), failures = Some(List()))

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(claimant, claimantClaimingSocialSecurityBenefit), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(4737.70)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(748.80)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(5504.20)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(547.50)
    }

    "(no tapering required, joint claimants, both claimants claiming social security benefit) Determine the net amounts of a the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 100000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val claimantClaimingSocialSecurityBenefit1 = models.input.tc.Claimant(qualifying = true, doesNotTaper = true, isPartner = false, claimantElements = ClaimantDisability(), failures = Some(List()))
      val claimantClaimingSocialSecurityBenefit2 = models.input.tc.Claimant(qualifying = true, doesNotTaper = true, isPartner = true, claimantElements = ClaimantDisability(), failures = Some(List()))

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(claimantClaimingSocialSecurityBenefit1, claimantClaimingSocialSecurityBenefit2), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(4737.70)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(748.80)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(5504.20)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(547.50)
    }

    "(tapering required, joint claimants, both not claiming social security benefit) Determine the net amounts of a the period" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 100000.00
      val wtcThreshold = TCConfig.getConfig(fromDate).thresholds.wtcIncomeThreshold
      val ctcThreshold = TCConfig.getConfig(fromDate).thresholds.ctcIncomeThreshold
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val claimant1 = models.input.tc.Claimant(qualifying = true, isPartner = false, claimantElements = ClaimantDisability(), failures = Some(List()))
      val claimant2 = models.input.tc.Claimant(qualifying = true, isPartner = true, claimantElements = ClaimantDisability(), failures = Some(List()))

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(claimant1, claimant2), children = List())

      val result = TCCalculator.calculator.generateRequiredAmountsPerPeriod(period, inputPeriod, income, wtcThreshold, ctcThreshold)
      result.elements.wtcWorkElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(0.00)
      result.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(0.00)
    }

    "generate the maximum amounts for a period model" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // period 1
          val tc = x.payload.eligibility.tc.getOrElse(throw new Exception())
          val p = tc.taxYears.head.periods.head
          val setup = TCCalculator.calculator.generateMaximumAmountsForPeriod(p)
          setup.elements.wtcWorkElement.maximumAmount shouldBe BigDecimal(2078.08)
          setup.elements.wtcChildcareElement.maximumAmount shouldBe BigDecimal(895.24)
          setup.elements.ctcIndividualElement.maximumAmount shouldBe BigDecimal(1455.42)
          setup.elements.ctcFamilyElement.maximumAmount shouldBe BigDecimal(286.50)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Generate the maximum amounts for a period model (no children, get just basic element)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_54.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // period 1
          val tc = x.payload.eligibility.tc.getOrElse(throw new Exception())
          val p = tc.taxYears.head.periods.head
          val setup = TCCalculator.calculator.generateMaximumAmountsForPeriod(p)
          setup.elements.wtcWorkElement.maximumAmount shouldBe BigDecimal(1025.67)
          setup.elements.wtcChildcareElement.maximumAmount shouldBe BigDecimal(0.00)
          setup.elements.ctcIndividualElement.maximumAmount shouldBe BigDecimal(0.00)
          setup.elements.ctcFamilyElement.maximumAmount shouldBe BigDecimal(0.00)
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(no tapering required) Taper the first element (WTC element)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_54.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // period 1
          val tc = x.payload.eligibility.tc.getOrElse(throw new Exception())
          val p = tc.taxYears.head.periods.head
          val income = tc.taxYears.head.houseHoldIncome
          val setup = TCCalculator.calculator.generateMaximumAmountsForPeriod(p)
          val i = TCCalculator.calculator.incomeForPeriod(income, p)
          val incomeThreshold = TCCalculator.calculator.wtcIncomeThresholdForPeriod(period = p)

          val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
          val fromDate = LocalDate.parse("2016-09-27", formatter)
          val untilDate = LocalDate.parse("2017-04-06", formatter)
          val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

          val output = models.output.tc.Period(
            from = fromDate,
            until = untilDate,
            elements = Elements(
              wtcWorkElement = Element(
                maximumAmount = 1025.67,
                taperAmount = 1025.67,
                netAmount = 0.00
              ),
              wtcChildcareElement = Element(
                maximumAmount = 0.00,
                taperAmount = 0.00,
                netAmount = 0.00
              ),
              ctcIndividualElement = Element(
                maximumAmount = 0.00,
                taperAmount = 0.00,
                netAmount = 0.00
              ),
              ctcFamilyElement = Element(
                maximumAmount = 0.00,
                taperAmount = 0.00,
                netAmount = 0.00
              )
            )
          )

          val taperedPeriodModel = TCCalculator.calculator.taperFirstElement(period = setup, inputPeriod = inputPeriod, income = i, wtcIncomeThreshold = incomeThreshold)
          taperedPeriodModel shouldBe output

        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "(full tapering) taper the first element (WTC work element)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 19000
      val wtcIncomeThreshold = 5600
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }


      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFirstAmount = TCCalculator.calculator.taperFirstElement(period, inputPeriod, income, wtcIncomeThreshold)
      taperFirstAmount.elements.wtcWorkElement.netAmount shouldBe BigDecimal(0.00)
      taperFirstAmount.elements.wtcWorkElement.taperAmount shouldBe BigDecimal(4737.70)
    }

    "Populate the output model's net amounts to be maximum amounts when no tapering is required" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val totalMaximumAmount = BigDecimal(400.00)
      val inputedModel = models.output.tc.Period(
        from = fromDate,
        until = toDate,
        elements = Elements(
          wtcWorkElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          wtcChildcareElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          ctcIndividualElement = Element(
            maximumAmount = BigDecimal(100.00)
          ),
          ctcFamilyElement = Element(
            maximumAmount = BigDecimal(100.00)
          )
        )
      )
      val decoratedOutputModel = PrivateMethod[models.output.tc.Period]('getPeriodAmount)
      val result = TCCalculator.calculator invokePrivate decoratedOutputModel(inputedModel, totalMaximumAmount, true)
      result.elements.wtcWorkElement.maximumAmount shouldBe BigDecimal(100.00)
      result.elements.wtcChildcareElement.maximumAmount shouldBe BigDecimal(100.00)
      result.elements.ctcIndividualElement.maximumAmount shouldBe BigDecimal(100.00)
      result.elements.ctcFamilyElement.maximumAmount shouldBe BigDecimal(100.00)
    }

    "(partial tapering) Taper the first element (WTC work element)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 19000
      val wtcIncomeThreshold = 5600
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 5837.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 748.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFirstAmount = TCCalculator.calculator.taperFirstElement(period, inputPeriod, income, wtcIncomeThreshold)
      taperFirstAmount.elements.wtcWorkElement.netAmount shouldBe BigDecimal(343.70)
      taperFirstAmount.elements.wtcWorkElement.taperAmount shouldBe BigDecimal(5494.00)
    }

    "determine if tapering second element (WTC childcare element) is required" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 19000
      val wtcIncomeThreshold = 5600
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 58.00,
              taperAmount = 5494
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperSecondAmount = TCCalculator.calculator.taperSecondElement(period, inputPeriod, income, wtcIncomeThreshold)
      taperSecondAmount.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(728.80)
      taperSecondAmount.elements.wtcChildcareElement.taperAmount shouldBe BigDecimal(0.00)
    }

    "(full tapering) taper the second element (WTC childcare element)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 19000
      val wtcIncomeThreshold = 5600
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperSecondAmount = TCCalculator.calculator.taperSecondElement(period, inputPeriod, income, wtcIncomeThreshold)
      taperSecondAmount.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(0.00)
      taperSecondAmount.elements.wtcChildcareElement.taperAmount shouldBe BigDecimal(728.80)
    }

    "(partial tapering) Taper the second element (WTC childcare element)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 19000
      val wtcIncomeThreshold = 5600
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 828.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperSecondAmount = TCCalculator.calculator.taperSecondElement(period, inputPeriod, income, wtcIncomeThreshold)
      taperSecondAmount.elements.wtcChildcareElement.netAmount shouldBe BigDecimal(72.50)
      taperSecondAmount.elements.wtcChildcareElement.taperAmount shouldBe BigDecimal(756.30)
    }

    "determine if tapering third element (CTC Child Element) is required" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 19000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 4000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 72.50,
              taperAmount = 756.30
            ),
            ctcIndividualElement = Element(
              maximumAmount = 5504.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperThirdAmount = TCCalculator.calculator.taperThirdElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold)
      taperThirdAmount._1.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(5504.20)
      taperThirdAmount._1.elements.ctcIndividualElement.taperAmount shouldBe BigDecimal(0.00)
      taperThirdAmount._2 shouldBe false
    }

    "(full tapering) taper the third element (CTC child element) when calculated CTC Threshold is greater than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)
      val income = 21000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 15000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 500.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperThirdAmount = TCCalculator.calculator.taperThirdElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold)
      taperThirdAmount._1.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(0.00)
      taperThirdAmount._1.elements.ctcIndividualElement.taperAmount shouldBe BigDecimal(500.20)
      taperThirdAmount._2 shouldBe true
    }

    "(full tapering) taper the third element (CTC child element) when calculated CTC Threshold is less than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 21000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 19500
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 500.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperThirdAmount = TCCalculator.calculator.taperThirdElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold)
      taperThirdAmount._1.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(0.00)
      taperThirdAmount._1.elements.ctcIndividualElement.taperAmount shouldBe BigDecimal(500.20)
      taperThirdAmount._2 shouldBe true
    }

    "(partial tapering) taper the third element (CTC child element) when calculated CTC Threshold is greater than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 21000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 15000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 1200.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperThirdAmount = TCCalculator.calculator.taperThirdElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold)
      taperThirdAmount._1.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(352.7)
      taperThirdAmount._1.elements.ctcIndividualElement.taperAmount shouldBe BigDecimal(847.50)
      taperThirdAmount._2 shouldBe true
    }

    "(partial tapering) taper the third element (CTC child element) when calculated CTC Threshold is less than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 21000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 19500
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 2500.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperThirdAmount = TCCalculator.calculator.taperThirdElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold)
      taperThirdAmount._1.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(1885.20)
      taperThirdAmount._1.elements.ctcIndividualElement.taperAmount shouldBe BigDecimal(615.00)
      taperThirdAmount._2 shouldBe true
    }

    "(partial tapering) taper the third element (CTC child element) when calculated CTC Threshold is greater than ctcIncomeThreshold and income" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 12000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 11000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperThirdAmount = TCCalculator.calculator.taperThirdElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold)
      taperThirdAmount._1.elements.ctcIndividualElement.netAmount shouldBe BigDecimal(3400.20)
      taperThirdAmount._1.elements.ctcIndividualElement.taperAmount shouldBe BigDecimal(0.00)
      taperThirdAmount._2 shouldBe true
    }

    "determine if tapering fourth element (CTC family element)is required" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 12000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 11000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFourthAmount = TCCalculator.calculator.taperFourthElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, false)
      taperFourthAmount.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(547.50)
      taperFourthAmount.elements.ctcFamilyElement.taperAmount shouldBe BigDecimal(0.00)
    }

    "(full tapering) taper fourth element (CTC family element) when calculated CTC threshold is greater than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 30000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 11000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 547.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFourthAmount = TCCalculator.calculator.taperFourthElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, true)
      taperFourthAmount.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(0.00)
      taperFourthAmount.elements.ctcFamilyElement.taperAmount shouldBe BigDecimal(547.50)
    }

    "(full tapering) taper fourth element (CTC family element) when calculated CTC threshold is lesser than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 30000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 29000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 350.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFourthAmount = TCCalculator.calculator.taperFourthElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, true)
      taperFourthAmount.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(0.00)
      taperFourthAmount.elements.ctcFamilyElement.taperAmount shouldBe BigDecimal(350.50)
    }

    "(patrial tapering) taper fourth element (CTC family element) when calculated CTC threshold is greater than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 30000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 11000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 1300.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFourthAmount = TCCalculator.calculator.taperFourthElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, true)
      taperFourthAmount.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(163.20)
      taperFourthAmount.elements.ctcFamilyElement.taperAmount shouldBe BigDecimal(1137.30)
    }


    "(partial tapering) taper fourth element (CTC family element) when calculated CTC threshold is lesser than ctcIncomeThreshold" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 30000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 29000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 450.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFourthAmount = TCCalculator.calculator.taperFourthElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, true)
      taperFourthAmount.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(40.50)
      taperFourthAmount.elements.ctcFamilyElement.taperAmount shouldBe BigDecimal(410.00)
    }

    "(partial tapering) taper fourth element (CTC family element) when calculated CTC threshold is greater than ctcIncomeThreshold and income" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val income = 25000
      val wtcIncomeThreshold = 5600
      val ctcIncomeThreshold = 11000
      val period = {
        models.output.tc.Period(
          from = fromDate,
          until = untilDate,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 1300.50
            )
          )
        )
      }

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val taperFourthAmount = TCCalculator.calculator.taperFourthElement(period, inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, true)
      taperFourthAmount.elements.ctcFamilyElement.netAmount shouldBe BigDecimal(1300.50)
      taperFourthAmount.elements.ctcFamilyElement.taperAmount shouldBe BigDecimal(0.00)
    }

    "Determine that the list of periods is not populated when the period list is empty" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-09-27", formatter)
      val untilDate = LocalDate.parse("2017-04-06", formatter)

      val tcEligibility = TCEligibility(
        taxYears = List(
          TaxYear(
            from = fromDate,
            until = untilDate,
            houseHoldIncome = BigDecimal(0.00),

            periods = List()
          ))
      )
      val request = Request(payload = Payload(
        eligibility = Eligibility(tc = Some(tcEligibility), tfc = None, esc = None)
      )
      )
      val taxYear = request.getTaxCreditsEligibility.get.taxYears.head
      val income = request.payload.eligibility.tc.get.taxYears.head.houseHoldIncome
      val setup = TCCalculator.calculator.getCalculatedPeriods(taxYear, income)

      setup shouldBe Nil
    }

    "Determine that the list of periods is populated when there is one period" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-09-27", formatter)
      val toDate = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // period 1
          val tc = x.payload.eligibility.tc.getOrElse(throw new Exception())
          val taxYear = x.getTaxCreditsEligibility.get.taxYears.head
          val income = x.payload.eligibility.tc.get.taxYears.head.houseHoldIncome
          val setup = TCCalculator.calculator.getCalculatedPeriods(taxYear, income)

          setup.head should not be Nil
          setup.head.from shouldBe fromDate
          setup.head.until shouldBe toDate
          setup.head.periodNetAmount shouldBe 2445.03
          setup.head.elements.ctcFamilyElement should not be Nil
          setup.head.elements.wtcWorkElement should not be Nil
          setup.head.elements.ctcIndividualElement should not be Nil
          setup.head.elements.wtcChildcareElement should not be Nil

          setup.tail shouldBe Nil
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine that the list of periods is populated when there are multiple periods" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val firstPeriodTo = LocalDate.parse("2016-12-12", formatter)
      val secondPeriodFrom = LocalDate.parse("2016-12-12", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          // period 1
          val tc = x.payload.eligibility.tc.getOrElse(throw new Exception())
          val taxYear = x.getTaxCreditsEligibility.get.taxYears.head
          val income = x.payload.eligibility.tc.get.taxYears.head.houseHoldIncome
          val setup = TCCalculator.calculator.getCalculatedPeriods(taxYear, income)

          setup.head should not be Nil
          setup.head.from shouldBe firstPeriodFrom
          setup.head.until shouldBe firstPeriodTo
          setup.head.periodNetAmount shouldBe 1795.98
          setup.head.elements.ctcFamilyElement should not be Nil
          setup.head.elements.wtcWorkElement should not be Nil
          setup.head.elements.ctcIndividualElement should not be Nil
          setup.head.elements.wtcChildcareElement should not be Nil

          setup.tail should not be Nil
          setup.tail.head.from shouldBe secondPeriodFrom
          setup.tail.head.until shouldBe secondPeriodTo
          setup.tail.head.periodNetAmount shouldBe 0.00
          setup.tail.head.elements.ctcFamilyElement should not be Nil
          setup.tail.head.elements.wtcWorkElement should not be Nil
          setup.tail.head.elements.ctcIndividualElement should not be Nil
          setup.tail.head.elements.wtcChildcareElement should not be Nil
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine total award for calculation (one period)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val firstPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val setup = TCCalculator.calculator.award(x)

          setup should not be Nil
          setup.tc.get.from shouldBe firstPeriodFrom
          setup.tc.get.until shouldBe firstPeriodTo
          setup.tc.get.totalAwardAmount shouldBe 2445.03
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine total award for calculation (two periods)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_52.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val setup = TCCalculator.calculator.award(x)

          setup should not be Nil
          setup.tc.get.from shouldBe firstPeriodFrom
          setup.tc.get.until shouldBe secondPeriodTo
          setup.tc.get.totalAwardAmount shouldBe 1795.98
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine advice amount for calculation (malformed input model, no tc eligibility present)" in {
      val request = Request(payload = Payload(
        eligibility = Eligibility(tc = None, tfc = None, esc = None)
      )
      )
      val setup = TCCalculator.calculator.incomeAdvice(request)
      setup.flatMap {
        result => result shouldBe AwardPeriod()
      }
    }

    "Determine total award for calculation (malformed input model, no tc eligibility present)" in {
      val request = Request(payload = Payload(
        eligibility = Eligibility(tc = None, tfc = None, esc = None)
      )
      )
      val setup = TCCalculator.calculator.award(request)
      setup.flatMap {
        result => result shouldBe AwardPeriod()
      }
    }

    "Determine calculation amount for the award amount" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val setup = TCCalculator.calculator.award(x)
          setup should not be Nil
          setup.tc.get.from shouldBe firstPeriodFrom
          setup.tc.get.until shouldBe secondPeriodTo
          setup.tc.get.totalAwardAmount shouldBe 2445.03
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine calculation amount for the award amount (Multiple Tax years)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2017/scenario_3.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2017-09-27", formatter)
      val lastPeriodTo = LocalDate.parse("2018-02-15", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val setup = TCCalculator.calculator.award(x)
          setup should not be Nil
          setup.tc.get.from shouldBe firstPeriodFrom
          setup.tc.get.until shouldBe lastPeriodTo
          setup.tc.get.totalAwardAmount shouldBe 10332.58
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Determine advice amount (including just the WTC threshold)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val maxHouseholdAmount = 15000.00
      val wtcThresholdPerPeriod = 6420

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val result = TCCalculator.calculator.getAdviceCalculationRounded(maxHouseholdAmount, wtcThresholdPerPeriod, inputPeriod)
      result shouldBe 43005.00
    }

    "Determine advice amount (including just the WTC threshold with decimal values)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val maxHouseholdAmount = 13425.64
      val wtcThresholdPerPeriod = 6420

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val result = TCCalculator.calculator.getAdviceCalculationRounded(maxHouseholdAmount, wtcThresholdPerPeriod, inputPeriod)
      result shouldBe 39165.14
    }

    "Determine advice amount (including just the WTC threshold value as 0)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val maxHouseholdAmount = 0.00
      val wtcThresholdPerPeriod = 6420

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val result = TCCalculator.calculator.getAdviceCalculationRounded(maxHouseholdAmount, wtcThresholdPerPeriod, inputPeriod)
      result shouldBe 6420
    }

    "Determine advice amount (including just the WTC threshold having negative value)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val fromDate = LocalDate.parse("2016-05-01", formatter)
      val untilDate = LocalDate.parse("2016-05-21", formatter)

      val maxHouseholdAmount = -9452.12
      val wtcThresholdPerPeriod = 6420

      val inputPeriod = models.input.tc.Period(from = fromDate, until = untilDate, householdElements = HouseHoldElements(), claimants = List(), children = List())

      val result = TCCalculator.calculator.getAdviceCalculationRounded(maxHouseholdAmount, wtcThresholdPerPeriod, inputPeriod)
      result shouldBe -16633.73
    }

    "Return advice amount set in the total award model of TCCalculation (Successful)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val setup = TCCalculator.calculator.incomeAdvice(x)
          setup should not be Nil
          setup.tc.get.from shouldBe firstPeriodFrom
          setup.tc.get.until shouldBe secondPeriodTo
          setup.tc.get.totalAwardAmount shouldBe 0.00
          setup.tc.get.houseHoldAdviceAmount shouldBe 14860.17
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Return advice amount set in the total award model of TCCalculation (Successful) (Multiple Tax years)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2017/scenario_3.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2017-09-27", formatter)
      val lastPeriodTo = LocalDate.parse("2018-02-15", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val setup = TCCalculator.calculator.incomeAdvice(x)
          setup should not be Nil
          setup.tc.get.from shouldBe firstPeriodFrom
          setup.tc.get.until shouldBe lastPeriodTo
          setup.tc.get.totalAwardAmount shouldBe 0.00
          setup.tc.get.houseHoldAdviceAmount shouldBe 40991.72
        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "Return advice amount set in the total award model of TCCalculation (No Eligibility present)" in {
      val request = Request(payload = Payload(
        eligibility = Eligibility(tc = None, tfc = None, esc = None)
      )
      )
      val setup = TCCalculator.calculator.incomeAdvice(request)
      setup should not be Nil
      setup.tc shouldBe None
    }

    "Determine total Maximum amount for a period (populated model) " in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)

      val period =
        models.output.tc.Period(
          from = firstPeriodFrom,
          until = secondPeriodTo,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 1300.50
            )
          )
        )
      val totalMaximumAmount = TCCalculator.calculator.getTotalMaximumAmountPerPeriod(period)
      totalMaximumAmount shouldBe 10167.20

    }

    "Determine total Maximum amount for a period (empty model)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)

      val period = {
        models.output.tc.Period(
          from = firstPeriodFrom,
          until = secondPeriodTo,
          elements = Elements(
            wtcWorkElement = Element(),
            wtcChildcareElement = Element(),
            ctcIndividualElement = Element(),
            ctcFamilyElement = Element()
          )
        )
      }
      val totalMaximumAmount = TCCalculator.calculator.getTotalMaximumAmountPerPeriod(period)
      totalMaximumAmount shouldBe 0.00
    }

    "Populate Period model when calculating household advice" in {
      val adviceAmount = BigDecimal(10000.00)
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      val period = {
        models.output.tc.Period(
          from = firstPeriodFrom,
          until = secondPeriodTo,
          elements = Elements(
            wtcWorkElement = Element(
              maximumAmount = 4737.70,
              netAmount = 0.00,
              taperAmount = 4737.70
            ),
            wtcChildcareElement = Element(
              maximumAmount = 728.80,
              netAmount = 0.00,
              taperAmount = 728.80
            ),
            ctcIndividualElement = Element(
              maximumAmount = 3400.20,
              netAmount = 0.00
            ),
            ctcFamilyElement = Element(
              maximumAmount = 1300.50
            )
          )
        )
      }
      val decoratedAdvicePeriod = PrivateMethod[models.output.tc.Period]('getPeriodAmount)
      val result = TCCalculator.calculator invokePrivate decoratedAdvicePeriod(period, adviceAmount, false)
      result should not be Nil
      result.periodNetAmount shouldBe 0.00
      result.periodAdviceAmount shouldBe 10000.00
    }

    "populate tax years model when there is only one tax year" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val listOfTaxYears = TCCalculator.calculator.getCalculatedTaxYears(x.getTaxCreditsEligibility.get)
          listOfTaxYears should not be Nil
          listOfTaxYears.head.from shouldBe firstPeriodFrom
          listOfTaxYears.head.until shouldBe secondPeriodTo
          listOfTaxYears.head.taxYearAwardAmount should not be BigDecimal(0.00)
          listOfTaxYears.head.periods should not be Nil

        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "populate tax years model when there are two tax years (Total Award Calculation)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2017/scenario_3.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2017-09-27", formatter)
      val lastPeriodTo = LocalDate.parse("2018-02-15", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val listOfTaxYears = TCCalculator.calculator.getCalculatedTaxYears(x.getTaxCreditsEligibility.get)
          listOfTaxYears should not be Nil
          listOfTaxYears.head.from shouldBe firstPeriodFrom
          listOfTaxYears.tail.head.until shouldBe lastPeriodTo
          listOfTaxYears.head.taxYearAwardAmount should not be BigDecimal(0.00)
          listOfTaxYears.head.periods should not be Nil

        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "populate tax years model when there is only one tax year (Income Advice Calculation)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2016/scenario_1.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2016-09-27", formatter)
      val secondPeriodTo = LocalDate.parse("2017-04-06", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val listOfTaxYears = TCCalculator.calculator.getCalculatedTaxYears(x.getTaxCreditsEligibility.get, true)
          listOfTaxYears should not be Nil
          listOfTaxYears.head.from shouldBe firstPeriodFrom
          listOfTaxYears.head.until shouldBe secondPeriodTo
          listOfTaxYears.head.taxYearAwardAmount shouldBe BigDecimal(0.00)
          listOfTaxYears.head.taxYearAdviceAmount should not be BigDecimal(0.00)

          listOfTaxYears.head.periods should not be Nil

        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }

    "populate tax years model when there are two tax years (Income Advice Calculation)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/tc/input/2017/scenario_3.json")
      val json: JsValue = Json.parse(resource.toString)
      val result = json.validate[Request]
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val firstPeriodFrom = LocalDate.parse("2017-09-27", formatter)
      val lastPeriodTo = LocalDate.parse("2018-02-15", formatter)
      logResult(result)
      result match {
        case JsSuccess(x, _) =>
          val listOfTaxYears = TCCalculator.calculator.getCalculatedTaxYears(x.getTaxCreditsEligibility.get, true)
          listOfTaxYears should not be Nil
          listOfTaxYears.head.from shouldBe firstPeriodFrom
          listOfTaxYears.tail.head.until shouldBe lastPeriodTo
          listOfTaxYears.head.taxYearAwardAmount shouldBe BigDecimal(0.00)
          listOfTaxYears.head.taxYearAdviceAmount should not be BigDecimal(0.00)

          listOfTaxYears.head.periods should not be Nil

        case JsError(e) => throw new RuntimeException(e.toList.toString)
      }
    }


    "(Proratering) determine the tax year to be proratered when proRata date is within first tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2016-07-06",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe true
      tcTaxYearToProRata._2.get shouldBe taxYear1
    }

    "(Proratering) determine the tax year to be proratered when proRata date is not in any tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2075-07-06",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe false
      tcTaxYearToProRata._2 shouldBe None
    }

    "(Proratering) determine the tax year to be proratered when proRata date is within second tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2017-05-06",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2018-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )
      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe true
      tcTaxYearToProRata._2.get shouldBe taxYear2
    }

    "(Proratering) determine the tax year to be proratered when proRata date is same as the end date of first tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2017-04-06",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-05",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-05",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe false
      tcTaxYearToProRata._2 shouldBe None
    }


    "(Proratering) determine the tax year to be proratered when proRata date is same as the end date of second tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2017-04-06",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-05",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-05",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe false
      tcTaxYearToProRata._2 shouldBe None
    }

    "(Proratering) determine the tax year to be proratered when proRata date is before as the start date of first tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2016-04-26",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe false
      tcTaxYearToProRata._2 shouldBe None
    }

    "(Proratering) determine the tax year to be proratered when proRata date is after as the end date of second tax year " in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataDate = LocalDate.parse ("2017-05-25",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataDate)
      tcTaxYearToProRata._1 shouldBe false
      tcTaxYearToProRata._2 shouldBe None
    }

    "(Proratering) determine the prorata award amount for tax year when prorata date is within the first tax year" in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataStartDate = LocalDate.parse ("2016-05-10",formatter)
      val proRataEndDate = LocalDate.parse ("2016-09-25",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataEndDate)
      tcTaxYearToProRata._1 shouldBe true
      tcTaxYearToProRata._2.get shouldBe taxYear1
      val outputTaxYearProrated = TCCalculator.calculator.proRataTaxYear(tcTaxYearToProRata._2.get, proRataStartDate, proRataEndDate)
      outputTaxYearProrated.proRataEnd shouldBe Some(proRataEndDate)
      outputTaxYearProrated.taxYearAwardAmount shouldBe BigDecimal(8000)
      outputTaxYearProrated.taxYearAwardProRataAmount shouldBe BigDecimal(3335.35)
      outputTaxYearProrated.taxYearAdviceAmount shouldBe BigDecimal(25000)
      outputTaxYearProrated.taxYearAdviceProRataAmount shouldBe BigDecimal(10422.96)
    }

    "(Proratering) determine the prorata award amount for tax year when prorata date is within the second tax year" in  {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataStartDate = LocalDate.parse ("2017-04-06",formatter)
      val proRataEndDate = LocalDate.parse ("2017-08-27",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2018-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataEndDate)
      tcTaxYearToProRata._1 shouldBe true
      tcTaxYearToProRata._2.get shouldBe taxYear2
      val outputTaxYearProrated = TCCalculator.calculator.proRataTaxYear(tcTaxYearToProRata._2.get, proRataStartDate, proRataEndDate)
      outputTaxYearProrated.proRataEnd shouldBe Some(proRataEndDate)
      outputTaxYearProrated.taxYearAwardAmount shouldBe BigDecimal(5000)
      outputTaxYearProrated.taxYearAwardProRataAmount shouldBe BigDecimal(1958.91)
      outputTaxYearProrated.taxYearAdviceAmount shouldBe BigDecimal(21000)
      outputTaxYearProrated.taxYearAdviceProRataAmount shouldBe BigDecimal(8227.40)
    }

    "(Prorataring) swap out proratad tax year(first tax year) in an award model" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataStartDate = LocalDate.parse ("2016-05-10",formatter)
      val proRataEndDate = LocalDate.parse ("2016-09-25",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2017-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      val tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataEndDate)
      tcTaxYearToProRata._1 shouldBe true
      tcTaxYearToProRata._2.get shouldBe taxYear1
      val outputTaxYearProrated = TCCalculator.calculator.proRataTaxYear(tcTaxYearToProRata._2.get, proRataStartDate, proRataEndDate)

      val calculationAward = models.output.tc.TCCalculation(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        totalAwardAmount = BigDecimal(13000.00),
        totalAwardProRataAmount = BigDecimal(0.00),
        houseHoldAdviceAmount = BigDecimal(46000.00),
        totalHouseHoldAdviceProRataAmount = BigDecimal(0.00),
        taxYears = taxYears
      )

      val proRataAward = models.output.tc.TCCalculation(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = Some(proRataEndDate),
        totalAwardAmount = BigDecimal(13000.00),
        totalAwardProRataAmount = BigDecimal(8335.35),
        houseHoldAdviceAmount = BigDecimal(46000.00),
        totalHouseHoldAdviceProRataAmount = BigDecimal(31422.96),
        taxYears = List(outputTaxYearProrated, taxYear2) // update the tax years
      )

      val adjustedAward = TCCalculator.calculator.adjustAwardWithProRata(award = calculationAward, proRataTaxYear = outputTaxYearProrated)
      adjustedAward shouldBe proRataAward
    }

    "(Prorataring) swap out proratad tax year(second tax year) in an award model" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val proRataStartDate = LocalDate.parse ("2017-04-06",formatter)
      val proRataEndDate = LocalDate.parse ("2017-08-27",formatter)
      val taxYear1Start = LocalDate.parse ("2016-05-10",formatter)
      val taxYear1End = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2Start = LocalDate.parse ("2017-04-06",formatter)
      val taxYear2End = LocalDate.parse ("2018-04-06",formatter)

      val taxYear1 = models.output.tc.TaxYear(
        from = taxYear1Start,
        until = taxYear1End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(8000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(25000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYear2 = models.output.tc.TaxYear(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        taxYearAwardAmount = BigDecimal(5000),
        taxYearAwardProRataAmount = BigDecimal(0.00),
        taxYearAdviceAmount = BigDecimal(21000),
        taxYearAdviceProRataAmount = BigDecimal(0.00),
        periods = List()
      )

      val taxYears = List(taxYear1, taxYear2)

      def tcTaxYearToProRata = TCCalculator.calculator.determineTaxYearToProRata(taxYears, proRataEndDate)
      tcTaxYearToProRata._1 shouldBe true
      tcTaxYearToProRata._2.get shouldBe taxYear2
      val outputTaxYearProrated = TCCalculator.calculator.proRataTaxYear(tcTaxYearToProRata._2.get, proRataStartDate, proRataEndDate)

      val calculationAward = models.output.tc.TCCalculation(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = None,
        totalAwardAmount = BigDecimal(13000.00),
        totalAwardProRataAmount = BigDecimal(0.00),
        houseHoldAdviceAmount = BigDecimal(46000.00),
        totalHouseHoldAdviceProRataAmount = BigDecimal(0.00),
        taxYears = taxYears
      )

      val proRataAward = models.output.tc.TCCalculation(
        from = taxYear2Start,
        until = taxYear2End,
        proRataEnd = Some(proRataEndDate),
        totalAwardAmount = BigDecimal(13000.00),
        totalAwardProRataAmount = BigDecimal(9958.91),
        houseHoldAdviceAmount = BigDecimal(46000.00),
        totalHouseHoldAdviceProRataAmount = BigDecimal(33227.40),
        taxYears = List(taxYear1, outputTaxYearProrated) // update the tax years
      )

      val adjustedAward = TCCalculator.calculator.adjustAwardWithProRata(award = calculationAward, proRataTaxYear = outputTaxYearProrated)
      adjustedAward shouldBe proRataAward
    }

  }
}