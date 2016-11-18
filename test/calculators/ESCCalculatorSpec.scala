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

import akka.japi.Option.Some
import calculators.ESCCalculator.ESCCalculatorService
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import controllers.FakeCCCalculatorApplication
import models.input.APIModels.{Eligibility, Payload, Request}
import models.input.esc._
import models.output.OutputAPIModel.AwardPeriod
import models.output.esc.Savings
import models.utility.{CalculationNIBands, CalculationTaxBands}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{ESCConfig, JSONFactory, Periods}

import scala.concurrent.Future

class ESCCalculatorSpec extends UnitSpec with FakeCCCalculatorApplication {

  "ESCCalculator" should {

    "return an instance of ESCEligibilityService" in {
      val service = ESCCalculator
      service.calculator shouldBe a[ESCCalculatorService]
    }
  }

  "ESCCalculatorService" should {

    "return a Future[AwardPeriod] result" in {
      val service = ESCCalculator
      val result = service.calculator.award(Request(payload = Payload(eligibility = Eligibility(esc = Some(ESCEligibility(taxYears = List())), tc = None, tfc = None))))
      result.isInstanceOf[Future[AwardPeriod]] shouldBe true
    }

    "(TY2016) get personal allowance when tax code is not provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(11000.00)
    }

    "(TY2016) get revised personal allowance when tax code is not provided (gross < 100000) " in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(89000.00), taxCode = "", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(11000.00)
    }

    "(TY2016) get revised personal allowance when tax code is not provided (gross is 110000) " in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(110000.00), taxCode = "", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(6000.0)
    }

    "(TY2016) get revised personal allowance when tax code is not provided (gross is 140000) " in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(140000.00), taxCode = "", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter L is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1060L", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(10600.00)
    }

    "(TY2016) get personal allowance when tax code letter M is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1200m", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(12000.00)
    }

    "(TY2016) get personal allowance when tax code letter N is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1201N", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(12010.00)
    }

    "(TY2016) get personal allowance when tax code letter T is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1039T", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(10390.00)
    }

    "(TY2016) get personal allowance when tax code letter Y is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1103y", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(11030.00)
    }

    "(TY2016) get personal allowance when tax code letter BR is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "BR", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter D0 is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "d0", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter D1 is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "D1", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter NT is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "NT", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2017) get personal allowance when tax code is not provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2017-05-06", formatter)
      val periodEnd = LocalDate.parse("2018-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(11000.00)
    }

    "(TY2017) get personal allowance when tax code letter L is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2017-05-06", formatter)
      val periodEnd = LocalDate.parse("2018-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1100L", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(11000.00)
    }

    "(TY2017) get personal allowance when tax code letter BR is provided" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2017-05-06", formatter)
      val periodEnd = LocalDate.parse("2018-04-06", formatter)

      val service = ESCCalculator
      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "br", niCategory = "")
      val result = service.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
      result shouldBe BigDecimal(0.00)
    }

    "allocate tax earnings to 20% rate band if tax code is BR" in {
      val taxableEarnings = BigDecimal(10000)
      val PA = BigDecimal(0.00)
      val taxCode = "BR"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 10000.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate tax earnings to 40% rate band if tax code is D0" in {
      val taxableEarnings = BigDecimal(45000)
      val PA = BigDecimal(0.00)
      val taxCode = "D0"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 45000.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate tax earnings to 45% rate band if tax code is D1" in {
      val taxableEarnings = BigDecimal(60000)
      val PA = BigDecimal(0.00)
      val taxCode = "D1"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 60000.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate tax earnings to 0% rate band if tax code is NT" in {
      val taxableEarnings = BigDecimal(9000)
      val PA = BigDecimal(0.00)
      val taxCode = "NT"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0 rate band (earnings < Personal Allowance)" in {
      val taxableEarnings = BigDecimal(10000)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 10000, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0 rate band (earnings = Personal Allowance)" in {
      val taxableEarnings = BigDecimal(10600)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0 and basic rate bands (earnings > Personal Allowance)" in {
      val taxableEarnings = BigDecimal(10600.01)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 0.01, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0 and basic rate bands (earnings < basic rate ceiling)" in {
      val taxableEarnings = BigDecimal(42000)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31400, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic rate bands (earnings = basic rate ceiling)" in {
      val taxableEarnings = BigDecimal(42385)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31785, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic rate bands (earnings > basic rate ceiling)" in {
      val taxableEarnings = BigDecimal(42385.01)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 31385.01, higherRateBand = 0.0, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic and higher rate bands (earnings = higher rate ceiling)" in {
      val taxableEarnings = BigDecimal(150000)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 32000, higherRateBand = 107000, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic, higher and additional rate bands (earnings > higher rate ceiling)" in {
      val taxableEarnings = BigDecimal(150000.01)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 32000, higherRateBand = 107000, additionalRateBand = 0.01)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings < Personal Allowance, TY2016)" in {
      val taxableEarnings = BigDecimal(9000.00)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 9000.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings > Personal Allowance, TY2016)" in {
      val taxableEarnings = BigDecimal(12000.00)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 1000.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings > higher limit, TY2017)" in {
      val taxableEarnings = BigDecimal(50000.00)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 32000, higherRateBand = 7000, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings > additional rate limit, TY2016)" in {
      val taxableEarnings = BigDecimal(150000.01)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 32000, higherRateBand = 107000.00, additionalRateBand = 0.01)
      val result = ESCCalculator.calculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe outputModel
    }

    "determine exemption amount (pre 2011, relevant earnings = 0)" in {
      val relevantEarnings = BigDecimal(0.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2009", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe 0.00

    }

    "determine exemption amount for the 0% tax band Pre 2011 for earnings < 0 for TY2016" in {
      val relevantEarnings = BigDecimal(-10.00)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 0% tax band Pre 2011 for earnings = PA for TY2016" in {
      val relevantEarnings = BigDecimal(0.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 20% tax band Pre 2011 for earnings < basic rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(10600.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(243)
    }

    "determine exemption amount for the 40% tax band Pre 2011 for earnings < higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(75000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("05-04-2011", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the 45% tax band Pre 2011 for earnings > higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(155000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(243)
    }

    "determine exemption amount for the 0% tax band Post 2011 earnings < 0 for TY2016" in {
      val relevantEarnings = BigDecimal(-1.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 0% tax band Post 2011 earnings < 0 for TY2017" in {
      val relevantEarnings = BigDecimal(-1.00)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 0% tax band Post 2011 earnings = PA for TY2016" in {
      val relevantEarnings = BigDecimal(0.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 20% tax band Post 2011 earnings < Basic rate limit for TY2016" in {
      val relevantEarnings = BigDecimal(10600.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(243)
    }

    "determine exemption amount for the 20% tax band Post 2011 earnings = basic rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(42465.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(124.00)
    }


    "determine exemption amount for the 40% tax band Post 2011 earnings < higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(42465.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(1488)
    }

    "determine exemption amount for the 40% tax band Post 2011 earnings < higher rate ceiling for TY2017" in {
      val relevantEarnings = BigDecimal(43000.01)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("02-05-2017", formatter)
      val toDate = LocalDate.parse("01-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escStartDate = escStartDate, escAmountPeriod = Periods.Monthly)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(1488)
    }

    "determine exemption amount for the 40% tax band on edge Post 2011 earnings = higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(150000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("06-04-2011", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(124.00)
    }

    "determine exemption amount for the 45% tax band on edge Post 2011 earnings > higher rate ceiling for TY2017" in {
      val relevantEarnings = BigDecimal(150000.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("06-04-2011", formatter)
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(1320)
    }

    "determine exemption amount for the 45% tax band Post 2011 earnings < additional rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(155000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val claimant = models.input.esc.Claimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, income = Income(), elements = ClaimantElements(vouchers = true), escAmount = 200.00, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(1320)
    }

    "determine exemption amount for the BR tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(155000.00)
      val taxCode = "BR"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the BR tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "BR"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the D0 tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "D0"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the D0 tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(30000.00)
      val taxCode = "D0"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(1488)
    }

    "determine exemption amount for the D1 tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "D1"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the D1 tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "D1"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(1320)
    }

    "determine exemption amount for the NT tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "NT"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0)
    }

    "determine exemption amount for the NT tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "NT"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, ""))

      result shouldBe BigDecimal(0)
    }

    "determine tax amount for each band (Income is 50000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31865, higherRateBand = 7535, additionalRateBand = 0.00)
      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 6373, higherRateBand = 3014, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.calculateTaxPerBand(inputModel, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe outputModel
    }

    "determine tax amount for each band (Income is 150000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31865, higherRateBand = 107535.00, additionalRateBand = 0.00)
      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 6373, higherRateBand = 43014.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.calculateTaxPerBand(inputModel, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe outputModel
    }

    "determine tax amount for each band (Income is 9000.00)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationTaxBands(zeroRateBand = 9000.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.calculateTaxPerBand(inputModel, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe outputModel
    }

    "determine tax amount for each band (Income is 12000.00)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationTaxBands(zeroRateBand = 10600.00, basicRateBand = 1400.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 280.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculator.calculateTaxPerBand(inputModel, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe outputModel
    }

    "determine total tax due (Income 50000 Annual)" in {
      val inputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 6373, higherRateBand = 3014, additionalRateBand = 0.00)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalTaxDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(9387.00)
    }

    "determine total tax due (Income 9000 Annual)" in {
      val inputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.calculator.totalTaxDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "determine total tax due (Income 1667 Monthly)" in {
      val inputModel = CalculationTaxBands(zeroRateBand = 156.80, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalTaxDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(156.8)
    }

    "determine post salary sacrifice amount (taxable pay > max relief amount)" in {
      val taxablePay = BigDecimal(10.00)
      val maxRelief = BigDecimal(1.00)

      val result = ESCCalculator.calculator.subtractActualReliefFromIncome(taxablePay, maxRelief, Periods.Yearly)
      result shouldBe BigDecimal(9.00)
    }

    "determine post salary sacrifice amount (taxable pay < max relief amount)" in {
      val taxablePay = BigDecimal(100.00)
      val maxRelief = BigDecimal(101.00)

      val result = ESCCalculator.calculator.subtractActualReliefFromIncome(taxablePay, maxRelief, Periods.Yearly)
      result shouldBe BigDecimal(0.00)
    }

    "determine actual relief amount (maxRelief > voucher amount)" in {
      val escAmount = BigDecimal(200.00)
      val maxRelief = BigDecimal(250.00)

      val result = ESCCalculator.calculator.determineActualIncomeRelief(escAmount, maxRelief)
      result shouldBe escAmount
    }

    "determine actual relief amount (maxRelief = voucher amount)" in {
      val escAmount = BigDecimal(250.00)
      val maxRelief = BigDecimal(250.00)

      val result = ESCCalculator.calculator.determineActualIncomeRelief(escAmount, maxRelief)
      result shouldBe escAmount
    }

    "determine actual relief amount (maxRelief < voucher amount)" in {
      val escAmount = BigDecimal(300.00)
      val maxRelief = BigDecimal(250.00)

      val result = ESCCalculator.calculator.determineActualIncomeRelief(escAmount, maxRelief)
      result shouldBe maxRelief
    }

    "Determine relevant earnings (income is 9000) if taxable income is less than personal allowance" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val income = Income(taxablePay = BigDecimal(9000), gross = BigDecimal(9000))
      val result = ESCCalculator.calculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe BigDecimal(0.00)
    }

    "Determine relevant earnings (income is 12000) if taxable income is greater than personal allowance and less than higher rate ceiling" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val income = Income(taxablePay = BigDecimal(12000), gross = BigDecimal(12000))
      val result = ESCCalculator.calculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe BigDecimal(1000.00)
    }

    "Determine relevant earnings (income is 150000) if taxable income is greater than personal allowance and less than higher rate ceiling (150000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val income = Income(taxablePay = BigDecimal(150000), gross = BigDecimal(150000))
      val result = ESCCalculator.calculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe BigDecimal(139000.00)
    }

    "Determine relevant earnings (income is 160000) if gross income is greater than higher rate ceiling (150000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())
      val income = Income(taxablePay = BigDecimal(155000), gross = BigDecimal(160000)) //gross include pension amount of 50000
      val result = ESCCalculator.calculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, ""))
      result shouldBe BigDecimal(155000.00)
    }

    "calculate savings per claimant (single claimant, post 2011, gross < personal allowance, voucher amount < max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 9000.00, gross = 10000.00, taxCode = "", niCategory = ""), elements = ClaimantElements(vouchers = true), escStartDate = fromDate, escAmount = 100.00, escAmountPeriod = Periods.Monthly)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))

      val result = ESCCalculator.calculator.determineSavingsPerClaimant(List(inputClaimant), period)

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = false, income = models.output.esc.Income(taxablePay = 9000.00, gross = 10000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 100.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 0.00, niSaving = 0.00, totalSaving = 0.00), maximumRelief = 0.0, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(niPaid = 19.32), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(niPaid = 19.32))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (single claimant, post 2011, gross < basic rate limit, voucher amount > max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 12, income = Income(taxablePay = 40700, gross = 42700, taxCode = "", niCategory = "A"), elements = ClaimantElements(vouchers = true), escStartDate = fromDate, escAmount = 500.00, escAmountPeriod = Periods.Monthly)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))

      val result = ESCCalculator.calculator.determineSavingsPerClaimant(List(inputClaimant), period)

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 12, isPartner = false, income = models.output.esc.Income(taxablePay = 40700.00, gross = 42700.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 500.00, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 48.60, niSaving = 29.16, totalSaving = 77.76), maximumRelief = 243.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 495.0, niPaid = 346.32), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 446.40, niPaid = 317.16))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (single claimant, post 2011, gross < additional rate limit, voucher amount < max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "B"), elements = ClaimantElements(vouchers = true), escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))

      val result = ESCCalculator.calculator.determineSavingsPerClaimant(List(inputClaimant), period)

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "B"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 36.00, niSaving = 1.80 , totalSaving = 37.80), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 181.97), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 180.17))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (single claimant, post 2011, gross > additional rate limit, voucher amount > max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"), elements = ClaimantElements(vouchers = true), escStartDate = fromDate, escAmount = 500.00, escAmountPeriod = Periods.Monthly)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))

      val result = ESCCalculator.calculator.determineSavingsPerClaimant(List(inputClaimant), period)

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = false, income = models.output.esc.Income(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 500.00, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 44.0, niSaving = 2.2, totalSaving =46.2 ), maximumRelief = 110.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 4466.60, niPaid = 527.66), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 4422.60,niPaid = 525.46))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (two claimants, first - post 2011, gross > additional rate limit, voucher amount < max relief, second - post 2011, gross < additional rate limit, voucher amount < max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"), elements = ClaimantElements(vouchers = true), escStartDate = fromDate, escAmount = 80, escAmountPeriod = Periods.Monthly)
      val inputClaimant2 = Claimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 60000, gross = 60000, taxCode = "", niCategory = "B"), elements = ClaimantElements(vouchers = true), escStartDate = fromDate, escAmount = 120, escAmountPeriod = Periods.Monthly)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))

      val result = ESCCalculator.calculator.determineSavingsPerClaimant(List(inputClaimant, inputClaimant2), period)

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = false, income = models.output.esc.Income(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 80, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 32.00, niSaving = 1.6, totalSaving = 33.6 ), maximumRelief = 110.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 4466.60,niPaid = 527.66), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 4434.60, niPaid = 526.06))

      val outputClaimant2 = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = true, income = models.output.esc.Income(taxablePay = 60000, gross = 60000, taxCode = "", niCategory = "B"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 120, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 48, niSaving = 2.4 , totalSaving = 50.4 ), maximumRelief = 124, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 1099.80,niPaid = 198.63), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 1051.80, niPaid = 196.23))

      result shouldBe List(outputClaimant, outputClaimant2)
    }


    "calculate savings per claimant (single claimant, pre 2011, gross > additional rate limit, voucher amount > max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-05-2009", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"), elements = ClaimantElements(vouchers = true), escStartDate = escStartDate, escAmount = 500.00, escAmountPeriod = Periods.Monthly)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))

      val result = ESCCalculator.calculator.determineSavingsPerClaimant(List(inputClaimant), period)

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = false, income = models.output.esc.Income(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 500, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate,
        savings = Savings(taxSaving = 97.20, niSaving = 4.86, totalSaving = 102.06), maximumRelief = 243.0, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 4466.60, niPaid = 527.66), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 4369.40, niPaid = 522.80))

      result shouldBe List(outputClaimant)
    }

    "calculate tax and NI savings per claimant (single period, one claimant, 0 eligible months) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val inputClaimant = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 0.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))
      val taxYear = TaxYear(startDate = fromDate, endDate = toDate, periods = List(period))

      val outputClaimant = models.output.esc.Claimant(qualifying = false, eligibleMonthsInTaxYear = 0, isPartner = false, income = models.output.esc.Income(niCategory = "A"),
        elements = models.output.esc.ClaimantElements(), escAmount = 0.0, escAmountPeriod = Periods.Monthly,escStartDate = fromDate,
        savings = Savings(), maximumRelief = 0.0, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI())

      val outputTY = models.output.esc.TaxYear(fromDate, toDate, Savings(), List(outputClaimant))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax and NI savings per claimant (single period, one claimant, 9 eligible months) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val inputClaimant = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 9, income = Income(), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 0.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))
      val taxYear = TaxYear(startDate = fromDate, endDate = toDate, periods = List(period))

      val outputClaimant = models.output.esc.Claimant(qualifying = false, eligibleMonthsInTaxYear = 9, isPartner = false, income = models.output.esc.Income(niCategory = "A"),
        elements = models.output.esc.ClaimantElements(), escAmount = 0.0, escAmountPeriod = Periods.Monthly,escStartDate = fromDate,
        savings = Savings(), maximumRelief = 0.0, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI())

      val outputTY = models.output.esc.TaxYear(fromDate, toDate, Savings(), List(outputClaimant))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax and NI savings per claimant (period 1 - 9 eligible months, period 2 - 2 eligible months, one claimant) (taxablePay 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 9, income = Income(taxablePay = 50000.00, gross = 50000.00,taxCode = "", niCategory = "A"), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val inputClaimant2 = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 2, income = Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2))
      val taxYear = TaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 11, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(taxSaving = 396, niSaving = 19.8, totalSaving = 415.8), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.20))

      val outputTY = models.output.esc.TaxYear(fromDate, toDate, Savings(taxSaving = 396, niSaving = 19.8, totalSaving = 415.8), List(outputClaimant))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax savings per claimant (period 1 - 2 eligible months, period 2 - 0 eligible months, one claimant) (taxablePay 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 2, income = Income(taxablePay = 50000.00, gross = 50000.00, niCategory ="B"), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val inputClaimant2 = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00, niCategory = "B"), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2))
      val taxYear = TaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 2, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "B"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 75.6, taxSaving = 72, niSaving = 3.6), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 181.97), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 180.17))

      val outputTY = models.output.esc.TaxYear(fromDate, toDate, Savings(totalSaving = 75.6, taxSaving = 72, niSaving = 3.6), List(outputClaimant))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax savings per claimant (claimant 1 = period 1 - 10 eligible months, period 2 - 1 eligible months) (claimant 2 = period 1 - 1 eligible months, period 2 - 1 eligible months) (taxablePay both 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 10, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner = Claimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val inputClaimant2 = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner2 = Claimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputPartner))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2, inputPartner2))

      val taxYear = TaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 11, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 415.8, taxSaving = 396.00, niSaving = 19.8), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.20))

      val outputPartner = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 2, isPartner = true, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 75.6, taxSaving = 72.00, niSaving = 3.6), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.20))

      val outputTY = models.output.esc.TaxYear(fromDate, toDate, Savings(totalSaving = 491.4, taxSaving = 468.00, niSaving = 23.4), List(outputClaimant, outputPartner))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax savings per claimant (claimant 1 = period 1 - 0 eligible months, period 2 - 0 eligible months) (claimant 2 = period 1 - 0 eligible months, period 2 - 1 eligible months) (taxablePay both 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner = Claimant(qualifying = false, isPartner = true, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val inputClaimant2 = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner2 = Claimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputPartner))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2, inputPartner2))

      val taxYear = TaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.Claimant(qualifying = false, eligibleMonthsInTaxYear = 0, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = false), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 0.00, taxSaving = 0.00, niSaving = 0.00), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.20))

      val outputPartner = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = true, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.20))

      val outputTY = models.output.esc.TaxYear(fromDate, toDate, Savings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8), List(outputClaimant, outputPartner))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax savings per claimant (1st Tax Year = claimant 1 = period 1 - 0 eligible months, period 2 - 0 eligible months) (claimant 2 = period 1 - 0 eligible months, period 2 - 1 eligible months) (taxablePay both 50000.00) (Monthly)" +
      "(2st Tax Year = claimant 1 = period 1 - 1 eligible months, period 2 - 0 eligible months) (claimant 2 = period 1 - 0 eligible months, period 2 - 0 eligible months) (taxablePay both 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-01-2017", formatter)
      val toDate = LocalDate.parse("21-01-2017", formatter)
      val toDate2 = LocalDate.parse("06-04-2017", formatter)

      val inputClaimant = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner = Claimant(qualifying = false, isPartner = true, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00, niCategory ="B"), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val inputClaimant2 = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner2 = Claimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00, niCategory ="B"), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputPartner))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2, inputPartner2))

      val taxYear = TaxYear(startDate = fromDate, endDate = toDate2, periods = List(period, period2))

      //2nd TAX YEAR
      val fromDateTY2 = LocalDate.parse("06-04-2016", formatter)
      val fromDate2TY2 = LocalDate.parse("21-10-2016", formatter)
      val toDateTY2 = LocalDate.parse("21-10-2016", formatter)
      val toDate2TY2 = LocalDate.parse("21-12-2016", formatter)

      val inputClaimantTY2 = Claimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = true),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartnerTY2 = Claimant(qualifying = false, isPartner = true, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00, niCategory = "B"), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val inputClaimant2TY2 = Claimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)
      val inputPartner2TY2 = Claimant(qualifying = false, isPartner = true, eligibleMonthsInPeriod = 0, income = Income(taxablePay = 50000.00, gross = 50000.00, niCategory = "B"), elements = ClaimantElements(vouchers = false),
        escStartDate = fromDate, escAmount = 90.00, escAmountPeriod = Periods.Monthly)

      val periodTY2 = ESCPeriod(from = fromDateTY2, until = toDateTY2, claimants = List(inputClaimantTY2, inputPartnerTY2))
      val period2TY2 = ESCPeriod(from = fromDate2TY2, until = toDate2TY2, claimants = List(inputClaimant2TY2, inputPartner2TY2))

      val taxYear2 = TaxYear(startDate = fromDateTY2, endDate = toDate2TY2, periods = List(periodTY2, period2TY2))

      //Output claimants TY1
      val outputClaimant = models.output.esc.Claimant(qualifying = false, eligibleMonthsInTaxYear = 0, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = false), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 0.00, taxSaving = 0.00, niSaving = 0.00), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.2))

      val outputPartner = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = true, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "B"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 37.80, taxSaving = 36, niSaving = 1.8), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 181.97), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 180.17))

      //Output claimants TY2
      val outputClaimantTY2 = models.output.esc.Claimant(qualifying = true, eligibleMonthsInTaxYear = 1, isPartner = false, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        elements = models.output.esc.ClaimantElements(vouchers = true), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 37.80, taxSaving = 36, niSaving = 1.8), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 359.2))

      val outputPartnerTY2 = models.output.esc.Claimant(qualifying = false, eligibleMonthsInTaxYear = 0, isPartner = true, income = models.output.esc.Income(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "B"),
        elements = models.output.esc.ClaimantElements(vouchers = false), escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = Savings(totalSaving = 0.00, taxSaving = 0.00, niSaving = 0.00), maximumRelief = 124.00, maximumReliefPeriod = Periods.Monthly,
        taxAndNIBeforeSacrifice = models.output.esc.TaxAndNI(taxPaid = 766.60, niPaid = 181.97), taxAndNIAfterSacrifice = models.output.esc.TaxAndNI(taxPaid = 730.60, niPaid = 180.17))

      //result
      val outputTY = models.output.esc.TaxYear(fromDate, toDate2, Savings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8), List(outputClaimant, outputPartner))
      val outputTY2 = models.output.esc.TaxYear(fromDateTY2, toDate2TY2, Savings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8), List(outputClaimantTY2, outputPartnerTY2))
      val resultTY = ESCCalculator.calculator.getCalculatedTaxYears(List(taxYear, taxYear2))

      resultTY shouldBe List(outputTY, outputTY2)
    }

    // NI TESTS //
    "allocate gross earnings to NI bands (earnings in lower earnings level, TY2016, category A)" in {
      val grossPay = BigDecimal(485.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in primary earnings level, TY2016, category A)" in {
      val grossPay = BigDecimal(485.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 0.01, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in primary earnings level, TY2016, category B)" in {
      val grossPay = BigDecimal(672)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper accrual earnings level, TY2016, category B)" in {
      val grossPay = BigDecimal(672.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 0.01, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper accrual earnings level, TY2016, category C)" in {
      val grossPay = BigDecimal(3337.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper  earnings level, TY2016, category A)" in {
      val grossPay = BigDecimal(3337.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 0.01, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper  earnings level, TY2016, category C)" in {
      val grossPay = BigDecimal(3532.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 195, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in above upper earnings level, TY2016, category C)" in {
      val grossPay = BigDecimal(3532.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 195.01, aboveUpperEarningsBand = 0.0)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in above upper earnings level, TY2017, category B)" in {
      val grossPay = BigDecimal(10000.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 246, aboveUpperEarningsBand = 6417)
      val result = ESCCalculator.calculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 485 (lower earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 600 (primary earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 115, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 672 (primary earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period, ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2017, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 128, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 15.36, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period, ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 128, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 7.4880, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2016, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 128, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 163, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 319.8, upperEarningsBand = 19.56, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 163, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 155.9025, upperEarningsBand = 9.5355, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2016, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 163, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 195, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 319.8, upperEarningsBand = 23.4, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 195, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 155.9025, upperEarningsBand = 11.4075, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period, ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2016, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 195, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 485 (lower earnings level), TY2017, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 600 (primary earnings level), TY2017, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 115, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 672 (primary earnings level), TY2017, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 128, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 15.36, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2017, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 163, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 155.9025, upperEarningsBand = 9.5355, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B"))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2017, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperAccrualEarningsBand = 2665, upperEarningsBand = 195, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C"))

      result shouldBe outputModel
    }

    "determine total NI due (Income 4000 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 319.8, upperEarningsBand = 23.4, aboveUpperEarningsBand = 9.36)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(352.56)
    }

    "determine total NI due (Income 3500 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 155.9025, upperEarningsBand = 9.5355, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(165.44)
    }

    "determine total NI due (Income 800 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 15.36, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(15.36)
    }

    "determine total NI due (Income 672 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0.0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "determine total NI due (Income 600 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0.00, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.0)
    }

    "determine total NI due (Income 485 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperAccrualEarningsBand = 0.00, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.calculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "determine total NI savings per month after salary sacrifice for niCategory A (income 4000, voucher amount 200)" in {
      val grossPay = BigDecimal(4000.00)
      val reliefAmount = BigDecimal(124.00)
      val calcPeriod = Periods.Yearly
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val result = ESCCalculator.calculator.calculateNISavings(period, grossPay, reliefAmount, ESCConfig.getConfig(period.from, "A"), calcPeriod)
      result._1 shouldBe BigDecimal(2.48)
      result._2 shouldBe BigDecimal(357.66)
      result._3 shouldBe BigDecimal(355.18)
    }


    "determine total NI savings per month after salary sacrifice for niCategory B (income 4000, voucher amount 200)" in {
      val grossPay = BigDecimal(4000.00)
      val reliefAmount = BigDecimal(124.00)
      val calcPeriod = Periods.Yearly
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val result = ESCCalculator.calculator.calculateNISavings(period, grossPay, reliefAmount, ESCConfig.getConfig(period.from, "B"), calcPeriod)
      result._1 shouldBe BigDecimal(2.48)
      result._2 shouldBe BigDecimal(178.63)
      result._3 shouldBe BigDecimal(176.15)
    }

    "determine total NI savings per month after salary sacrifice for niCategory A (income 3500, voucher amount 200)" in {
      val grossPay = BigDecimal(3500.00)
      val reliefAmount = BigDecimal(200.00)
      val calcPeriod = Periods.Yearly
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List())

      val result = ESCCalculator.calculator.calculateNISavings(period, grossPay, reliefAmount, ESCConfig.getConfig(period.from, "A"), calcPeriod)
      result._1 shouldBe BigDecimal(24)
      result._2 shouldBe BigDecimal(339.36)
      result._3 shouldBe BigDecimal(315.36)
    }

    "Generate total award with claimants (Total Award test)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/esc/input/calculator_input_test.json")
      val json: JsValue = Json.parse(resource.toString)
      val inputJson = json.validate[Request]
      inputJson.isInstanceOf[JsSuccess[Request]] shouldBe true

      val result : AwardPeriod = ESCCalculator.calculator.award(inputJson.get)

      val resourceJson = JsonLoader.fromResource("/json/esc/output/output_test_1.json")
      val outputJson: JsValue = Json.parse(resourceJson.toString)

      JSONFactory.generateResultJson(result) shouldBe outputJson
    }

    "Generate total award with claimants (Total Award test - empty award period)" in {
      val emptyRequest = Request(payload = Payload(eligibility = Eligibility(null, null, null)))
      val result : AwardPeriod = ESCCalculator.calculator.award(emptyRequest)

      result shouldBe AwardPeriod()
    }

    "Validate tax code and return personal allowance (11000)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1100L", niCategory = "")
      ESCCalculator.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim)) shouldBe 11000
    }

    "Validate tax code and return personal allowance (9999)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "9999M", niCategory = "")
      ESCCalculator.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim)) shouldBe 99990
    }

    "Validate tax code and return personal allowance (9Y)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "9Y", niCategory = "")
      ESCCalculator.calculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim)) shouldBe 90
    }

    "Validate tax code and return D1" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "d1", niCategory = "")
      ESCCalculator.calculator.getTaxCode(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim)) shouldBe "D1"
    }

    "Return error for invalid tax code (Y)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "Y", niCategory = "")
      try {
        val result = ESCCalculator.calculator.getTaxCode(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
        result shouldBe a[NoSuchElementException]
      } catch {
        case e: Exception =>
          e shouldBe a[NoSuchElementException]
      }
    }

    "Return error for invalid tax code (D11)" in {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List())
      val income = Income(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "D11", niCategory = "")
      try {
        val result = ESCCalculator.calculator.getTaxCode(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim))
        result shouldBe a[NoSuchElementException]
      } catch {
        case e: Exception =>
          e shouldBe a[NoSuchElementException]
      }
    }



  }
}