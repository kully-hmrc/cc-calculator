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

package calculators

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import models.input.esc._
import models.output.esc.{ESCCalculatorOutput, ESCSavings, ESCTaxAndNi}
import models.utility.{CalculationNIBands, CalculationTaxBands}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec
import utils.{ESCConfig, FakeCCCalculatorApplication, Periods}
import scala.concurrent.Future
import org.scalatest.mock.MockitoSugar

class ESCCalculatorSpec extends UnitSpec with FakeCCCalculatorApplication with MockitoSugar with org.scalatest.PrivateMethodTester {
  val location = "england"

  def buildChild(childCareCost: BigDecimal, childCareCostPeriod: Periods.Period = Periods.Monthly, qualifying: Boolean = true): Child = Child(
    qualifying = qualifying,
    childCareCost = childCareCost,
    childCareCostPeriod= childCareCostPeriod
  )

  "ESCCalculatorService" should {

    "return a Future[AwardPeriod] result" in {
      val result = ESCCalculator.award(ESCCalculatorInput(escTaxYears = List(), location = "england"))
      result.isInstanceOf[Future[ESCCalculatorOutput]] shouldBe true
    }

    "(TY2016) get revised personal allowance when tax code is not provided (gross < 100000) " in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(89000.00), taxCode = "", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(11000.00)
    }

    "(TY2016) get revised personal allowance when tax code is not provided (gross > 100000) " in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(110000.00), taxCode = "", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(6000.00)
    }

    "(TY2016) get personal allowance when tax code letter L is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1060L", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(10600.00)
    }

    "(TY2016) get personal allowance when tax code letter M is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1200m", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(12000.00)
    }

    "(TY2016) get personal allowance when tax code letter N is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1201N", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(12010.00)
    }

    "(TY2016) get personal allowance when tax code letter T is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1039T", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(10390.00)
    }

    "(TY2016) get personal allowance when tax code letter Y is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1103y", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(11030.00)
    }

    "(TY2016) get personal allowance when tax code letter BR is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "BR", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter D0 is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "d0", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter D1 is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "D1", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2016) get personal allowance when tax code letter NT is provided" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "NT", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(0.00)
    }

    "(TY2017) get personal allowance when tax code is not provided" in {
      val periodStart = LocalDate.parse("2017-05-06", formatter)
      val periodEnd = LocalDate.parse("2018-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "", niCategory = "")
      val result = ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
      result shouldBe BigDecimal(11500.00)
    }

    "allocate tax earnings to 20% rate band if tax code is BR" in {
      val taxableEarnings = BigDecimal(10000)
      val PA = BigDecimal(0.00)
      val taxCode = "BR"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 10000.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate tax earnings to 40% rate band if tax code is D0" in {
      val taxableEarnings = BigDecimal(45000)
      val PA = BigDecimal(0.00)
      val taxCode = "D0"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 45000.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate tax earnings to 45% rate band if tax code is D1" in {
      val taxableEarnings = BigDecimal(60000)
      val PA = BigDecimal(0.00)
      val taxCode = "D1"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 60000.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate tax earnings to 0% rate band if tax code is NT" in {
      val taxableEarnings = BigDecimal(9000)
      val PA = BigDecimal(0.00)
      val taxCode = "NT"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0 rate band (earnings < Personal Allowance)" in {
      val taxableEarnings = BigDecimal(10000)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val calcPeriod = Periods.Yearly

      val outputModel = CalculationTaxBands(zeroRateBand = 10000, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0 rate band (earnings = Personal Allowance)" in {
      val taxableEarnings = BigDecimal(10600)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0 and basic rate bands (earnings > Personal Allowance)" in {
      val taxableEarnings = BigDecimal(10600.01)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 0.01, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0 and basic rate bands (earnings < basic rate ceiling)" in {
      val taxableEarnings = BigDecimal(42000)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31400, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic rate bands (earnings = basic rate ceiling)" in {
      val taxableEarnings = BigDecimal(42385)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31785, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic rate bands (earnings > basic rate ceiling)" in {
      val taxableEarnings = BigDecimal(42385.01)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 31385.01, higherRateBand = 0.0, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic and higher rate bands (earnings = higher rate ceiling)" in {
      val taxableEarnings = BigDecimal(150000)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 32000, higherRateBand = 107000, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate earnings to 0, basic, higher and additional rate bands (earnings > higher rate ceiling)" in {
      val taxableEarnings = BigDecimal(150000.01)
      val PA = BigDecimal(11500)
      val taxCode = "1150L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11500, basicRateBand = 33500, higherRateBand = 105000, additionalRateBand = 0.01)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings < Personal Allowance, TY2016)" in {
      val taxableEarnings = BigDecimal(9000.00)
      val PA = BigDecimal(10600)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 9000.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings > Personal Allowance, TY2016)" in {
      val taxableEarnings = BigDecimal(12000.00)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 1000.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings > higher limit, TY2017)" in {
      val taxableEarnings = BigDecimal(50000.00)
      val PA = BigDecimal(11500)
      val taxCode = "1150L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11500, basicRateBand = 33500, higherRateBand = 5000, additionalRateBand = 0.00)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "allocate relevant earnings to 0, basic higher rate bands (earnings > additional rate limit, TY2016)" in {
      val taxableEarnings = BigDecimal(150000.01)
      val PA = BigDecimal(11000)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val outputModel = CalculationTaxBands(zeroRateBand = 11000, basicRateBand = 32000, higherRateBand = 107000.00, additionalRateBand = 0.01)
      val result = ESCCalculator.allocateAmountToTaxBands(taxableEarnings, PA, period, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe outputModel
    }

    "determine exemption amount (pre 2011, relevant earnings = 0)" in {
      val relevantEarnings = BigDecimal(0.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2009", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe 0.00

    }

    "determine exemption amount for the 0% tax band Pre 2011 for earnings < 0 for TY2016" in {
      val relevantEarnings = BigDecimal(-10.00)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 0% tax band Pre 2011 for earnings = PA for TY2016" in {
      val relevantEarnings = BigDecimal(0.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 20% tax band Pre 2011 for earnings < basic rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(10600.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(243)
    }

    "determine exemption amount for the 40% tax band Pre 2011 for earnings < higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(75000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("05-04-2011", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the 45% tax band Pre 2011 for earnings > higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(155000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(243)
    }

    "determine exemption amount for the 0% tax band Post 2011 earnings < 0 for TY2016" in {
      val relevantEarnings = BigDecimal(-1.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 0% tax band Post 2011 earnings < 0 for TY2017" in {
      val relevantEarnings = BigDecimal(-1.00)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 0% tax band Post 2011 earnings = PA for TY2016" in {
      val relevantEarnings = BigDecimal(0.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(0.00)
    }

    "determine exemption amount for the 20% tax band Post 2011 earnings < Basic rate limit for TY2016" in {
      val relevantEarnings = BigDecimal(10600.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(243)
    }

    "determine exemption amount for the 20% tax band Post 2011 earnings = basic rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(42465.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Monthly, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(124.00)
    }


    "determine exemption amount for the 40% tax band Post 2011 earnings < higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(42465.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(1488)
    }

    "determine exemption amount for the 40% tax band Post 2011 earnings < higher rate ceiling for TY2017" in {
      val relevantEarnings = BigDecimal(43000.01)
      val taxCode = "1100L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("02-05-2017", formatter)
      val toDate = LocalDate.parse("01-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, Periods.Yearly, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(1488)
    }

    "determine exemption amount for the 40% tax band on edge Post 2011 earnings = higher rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(150000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("06-04-2011", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(124.00)
    }

    "determine exemption amount for the 45% tax band on edge Post 2011 earnings > higher rate ceiling for TY2017" in {
      val relevantEarnings = BigDecimal(150000.01)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("06-04-2011", formatter)
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(1320)
    }

    "determine exemption amount for the 45% tax band Post 2011 earnings < additional rate ceiling for TY2016" in {
      val relevantEarnings = BigDecimal(155000.00)
      val taxCode = "1060L"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val claimant = models.input.esc.ESCClaimant(qualifying = true, eligibleMonthsInPeriod = 2, isPartner = false, previousIncome = None, currentIncome = None, vouchers = true,escStartDate = escStartDate)
      val pre2011 = claimant.isESCStartDateBefore2011
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(1320)
    }

    "determine exemption amount for the BR tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(155000.00)
      val taxCode = "BR"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the BR tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "BR"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the D0 tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "D0"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the D0 tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(30000.00)
      val taxCode = "D0"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe BigDecimal(1488)
    }

    "determine exemption amount for the D1 tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "D1"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe BigDecimal(2916)
    }

    "determine exemption amount for the D1 tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "D1"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe BigDecimal(1320)
    }

    "determine exemption amount for the NT tax code pre 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "NT"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2010", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = true
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe BigDecimal(0)
    }

    "determine exemption amount for the NT tax code Post 2011 earnings" in {
      val relevantEarnings = BigDecimal(79000.00)
      val taxCode = "NT"
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-06-2016", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val pre2011 = false
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.determineMaximumIncomeRelief(period, pre2011, relevantEarnings, calcPeriod, taxCode, ESCConfig.getConfig(period.from, "", location))

      result shouldBe BigDecimal(0)
    }

    "determine tax amount for each band (Income is 50000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val inputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31865, higherRateBand = 7535, additionalRateBand = 0.00)
      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 6373, higherRateBand = 3014, additionalRateBand = 0.00)
      val result = ESCCalculator.calculateTaxPerBand(inputModel, period, ESCConfig.getConfig(period.from, "", location))
      result shouldBe outputModel
    }

    "determine tax amount for each band (Income is 150000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())

      val inputModel = CalculationTaxBands(zeroRateBand = 10600, basicRateBand = 31865, higherRateBand = 107535.00, additionalRateBand = 0.00)
      val outputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 6373, higherRateBand = 43014.00, additionalRateBand = 0.00)
      val result = ESCCalculator.calculateTaxPerBand(inputModel, period, ESCConfig.getConfig(period.from, "", location))
      result shouldBe outputModel
    }

    "determine total tax due (Income < 10000 Annual)" in {
      val inputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 6373, higherRateBand = 3014, additionalRateBand = 0.00)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalTaxDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(9387.00)
    }

    "determine total tax due (Income = 0 Annual)" in {
      val inputModel = CalculationTaxBands(zeroRateBand = 0.00, basicRateBand = 0.00, higherRateBand = 0.00, additionalRateBand = 0.00)
      val calcPeriod = Periods.Monthly

      val result = ESCCalculator.totalTaxDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "determine post salary sacrifice amount (taxable pay > max relief amount)" in {
      val taxablePay = BigDecimal(10.00)
      val maxRelief = BigDecimal(1.00)

      val result = ESCCalculator.subtractActualReliefFromIncome(taxablePay, maxRelief, Periods.Yearly)
      result shouldBe BigDecimal(9.00)
    }

    "determine post salary sacrifice amount (taxable pay < max relief amount)" in {
      val taxablePay = BigDecimal(100.00)
      val maxRelief = BigDecimal(101.00)

      val result = ESCCalculator.subtractActualReliefFromIncome(taxablePay, maxRelief, Periods.Yearly)
      result shouldBe BigDecimal(0.00)
    }

    "determine actual relief amount (maxRelief > voucher amount)" in {
      val escAmount = BigDecimal(200.00)
      val maxRelief = BigDecimal(250.00)

      val result = ESCCalculator.determineActualIncomeRelief(escAmount, maxRelief)
      result shouldBe escAmount
    }

    "determine actual relief amount (maxRelief = voucher amount)" in {
      val escAmount = BigDecimal(250.00)
      val maxRelief = BigDecimal(250.00)

      val result = ESCCalculator.determineActualIncomeRelief(escAmount, maxRelief)
      result shouldBe escAmount
    }

    "determine actual relief amount (maxRelief < voucher amount)" in {
      val escAmount = BigDecimal(300.00)
      val maxRelief = BigDecimal(250.00)

      val result = ESCCalculator.determineActualIncomeRelief(escAmount, maxRelief)
      result shouldBe maxRelief
    }

    "Determine relevant earnings (income is 9000) if taxable income is less than personal allowance" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(9000), gross = BigDecimal(9000))
      val result = ESCCalculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(0.00)
    }

    "Determine relevant earnings (income is 12000) if taxable income is greater than personal allowance and less than higher rate ceiling" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(12000), gross = BigDecimal(12000))
      val result = ESCCalculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(1000.00)
    }

    "Determine relevant earnings (income is 150000) if taxable income is greater than personal allowance and less than higher rate ceiling (150000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(150000), gross = BigDecimal(150000))
      val result = ESCCalculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(138500.00)
    }

    "Determine relevant earnings (income is 160000) if gross income is greater than higher rate ceiling (150000)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(155000), gross = BigDecimal(160000)) //gross include pension amount of 50000
      val result = ESCCalculator.getAnnualRelevantEarnings(income, period, ESCConfig.getConfig(period.from, "", location))
      result shouldBe BigDecimal(155000.00)
    }

    "calculate savings per claimant (single claimant, post 2011, gross < personal allowance, voucher amount < max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 1, previousIncome = None,
        currentIncome = Some(ESCIncome(Some(10000.00), Some(95.00))), vouchers = true, escStartDate = fromDate)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 500)))

      val result = ESCCalculator.determineSavingsPerClaimant(period, location)

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = false,
        income = models.output.esc.ESCIncome(taxablePay = 8860.00,
          gross = 10000.00,
          taxCode = "",
          niCategory = "A"),
        vouchers = true, escAmount = 0.0,
        escAmountPeriod = Periods.Monthly,
        escStartDate = fromDate,
        savings = ESCSavings(taxSaving = 0.00,
          niSaving = 0.00, totalSaving = 0.00),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(niPaid = 19.32), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(niPaid = 19.32))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (single claimant, post 2011, gross < basic rate limit, voucher amount > max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 12, previousIncome = Some(ESCIncome(Some(42700.00), Some(100.00))), currentIncome = None, vouchers = true, escStartDate = fromDate)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 500)))

      val result = ESCCalculator.determineSavingsPerClaimant(period, location = location)

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 12,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 41500.00, gross = 42700.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 243.00, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(taxSaving = 48.60, niSaving = 29.16, totalSaving = 77.76),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 500.00, niPaid = 345.36), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 451.40, niPaid = 316.20))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (single claimant, post 2011, gross < additional rate limit, voucher amount < max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, previousIncome = Some(ESCIncome(Some(50000.00))), currentIncome = None, vouchers = true, escStartDate = fromDate)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 90)))

      val result = ESCCalculator.determineSavingsPerClaimant(period, location = location)

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true,
        eligibleMonthsInTaxYear = 1, isPartner = false,
        income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(37.80,36.00,1.80),
        ESCTaxAndNi(766.60,361.00),ESCTaxAndNi(730.60,359.20))

      Json.toJson(result) shouldBe Json.toJson(List(outputClaimant))
    }

    "calculate savings per claimant (single claimant, post 2011, gross > additional rate limit, voucher amount > max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, previousIncome = None,
        currentIncome = Some(ESCIncome(Some(150001))), vouchers = true, escStartDate = fromDate)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 500)))

      val result = ESCCalculator.determineSavingsPerClaimant(period, location = location)

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 110.00, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(taxSaving = 44.0, niSaving = 2.2, totalSaving =46.2 ),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4466.60, niPaid = 527.66), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4422.60,niPaid = 525.46))

      result shouldBe List(outputClaimant)
    }

    "calculate savings per claimant (two claimants, first - post 2011, gross > additional rate limit, voucher amount < max relief, second - post 2011, gross < additional rate limit, voucher amount < max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, previousIncome = None,
        currentIncome = Some(ESCIncome(Some(150001))), vouchers = true, escStartDate = fromDate)
      val inputClaimant2 = ESCClaimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1,
        previousIncome = Some(ESCIncome(Some(150001))),
        currentIncome = None, vouchers = true, escStartDate = fromDate)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputClaimant2), children = List(buildChild(childCareCost = 80), buildChild(childCareCost = 120)))

      val result = ESCCalculator.determineSavingsPerClaimant(period, location = location)

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 150001, gross = 150001, taxCode = "",
          niCategory = "A"),
        vouchers = true, escAmount = 90, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(taxSaving = 36.00, niSaving = 1.8, totalSaving = 37.80),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4466.60,niPaid = 527.66),
        taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4430.60, niPaid = 525.86))

      val outputClaimant2 = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = true, income = models.output.esc.ESCIncome(taxablePay = 150001, gross = 150001, taxCode = "",
          niCategory = "A"),
        vouchers = true, escAmount = 110, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(taxSaving = 44.00, niSaving = 2.2, totalSaving = 46.2),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4466.60,niPaid = 527.66),
        taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4422.60, niPaid = 525.46))

      result shouldBe List(outputClaimant, outputClaimant2)
    }



    "calculate savings per claimant (single claimant, pre 2011, gross > additional rate limit, voucher amount > max relief) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val escStartDate = LocalDate.parse("01-05-2009", formatter)
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1,
        previousIncome = Some(ESCIncome(Some(150001))),
        currentIncome = None, vouchers = true, escStartDate = escStartDate)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 500)))

      val result = ESCCalculator.determineSavingsPerClaimant(period, location = location)

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 150001, gross = 150001, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 243, escAmountPeriod = Periods.Monthly, escStartDate = escStartDate,
        savings = ESCSavings(taxSaving = 97.20, niSaving = 4.86, totalSaving = 102.06),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4466.60, niPaid = 527.66),
        taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 4369.40, niPaid = 522.80))

      result shouldBe List(outputClaimant)
    }

    "calculate tax and NI savings per claimant (single period, one claimant, 0 eligible months) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val inputClaimant = ESCClaimant(qualifying = false, isPartner = false, eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = None, vouchers = false,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List())
      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = false, eligibleMonthsInTaxYear = 0, isPartner = false,
        income = models.output.esc.ESCIncome(niCategory = "A"),
        vouchers = false, escAmount = 0.0, escAmountPeriod = Periods.Monthly,escStartDate = fromDate,
        savings = ESCSavings(),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi())

      val outputTY = models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(), List(outputClaimant))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax and NI savings per claimant (single period, one claimant, 9 eligible months) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val inputClaimant = ESCClaimant(qualifying = false, isPartner = false,
        eligibleMonthsInPeriod = 9, previousIncome = None, currentIncome = None, vouchers = false,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List())
      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = false, eligibleMonthsInTaxYear = 9,
        isPartner = false, income = models.output.esc.ESCIncome(niCategory = "A"),
        vouchers = false, escAmount = 0.0, escAmountPeriod = Periods.Monthly,escStartDate = fromDate,
        savings = ESCSavings(),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi())

      val outputTY = models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(), List(outputClaimant))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax and NI savings per claimant (period 1 - 9 eligible months, period 2 - 2 eligible months, one claimant) (taxablePay 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 9, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000.00))), vouchers = true,
        escStartDate = fromDate)

      val inputClaimant2 = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 2, previousIncome = Some(ESCIncome(Some(50000.00))),
        currentIncome = None, vouchers = true,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 90)))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2), children = List(buildChild(childCareCost = 90)))
      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 11,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00,
          taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(taxSaving = 396, niSaving = 19.8, totalSaving = 415.8),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 8349.80, niPaid = 4003.08), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 7953.80, niPaid = 3983.28))

      val outputTY = models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(taxSaving = 396, niSaving = 19.8, totalSaving = 415.8), List(outputClaimant))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      Json.toJson(resultTY) shouldBe Json.toJson(List(outputTY))
    }

    "calculate tax savings per claimant (period 1 - 2 eligible months, period 2 - 0 eligible months, one claimant) (taxablePay 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 2, previousIncome = Some(ESCIncome(Some(50000.00))),
        currentIncome = None, vouchers = true,
        escStartDate = fromDate)

      val inputClaimant2 = ESCClaimant(qualifying = false, isPartner = false,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = None, vouchers = false,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant), children = List(buildChild(childCareCost = 90)))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2), children = List(buildChild(childCareCost = 90)))
      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 2,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 75.6, taxSaving = 72, niSaving = 3.6),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 1533.2, niPaid = 722.00),
        taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 1461.20, niPaid = 718.40))

      val outputTY = models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(totalSaving = 75.6, taxSaving = 72, niSaving = 3.6), List(outputClaimant))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax savings per claimant (claimant 1 = period 1 - 10 eligible months, period 2 - 1 eligible months) (claimant 2 = period 1 - 1 eligible months," +
      " period 2 - 1 eligible months) (taxablePay both 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val fromDate2 = LocalDate.parse("21-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val toDate2 = LocalDate.parse("21-10-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 10, previousIncome = Some(ESCIncome(Some(50000))), currentIncome = None, vouchers = true,
        escStartDate = fromDate)
      val inputPartner = ESCClaimant(qualifying = true, isPartner = true,
        eligibleMonthsInPeriod = 1, previousIncome = Some(ESCIncome(Some(50000))), currentIncome = None, vouchers = true,
        escStartDate = fromDate)

      val inputClaimant2 = ESCClaimant(qualifying = true, isPartner = false, eligibleMonthsInPeriod = 1, previousIncome = None, currentIncome = None, vouchers = true,
        escStartDate = fromDate)
      val inputPartner2 = ESCClaimant(qualifying = true, isPartner = true, eligibleMonthsInPeriod = 1, previousIncome = None, currentIncome = None, vouchers = true,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputPartner), children = List(buildChild(childCareCost = 180)))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2, inputPartner2), children = List(buildChild(childCareCost = 180)))

      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 11,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 56.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 235.20, taxSaving = 224.00, niSaving = 11.2),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 7666.00, niPaid = 3610.00), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 7442.00, niPaid = 3598.8))

      val outputPartner = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 2, isPartner = true, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 124.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 52.08, taxSaving = 49.6, niSaving = 2.48),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 717.00, niPaid = 358.52))

      val outputTY = models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(totalSaving = 287.28, taxSaving = 273.6, niSaving = 13.68), List(outputClaimant, outputPartner))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      Json.toJson(resultTY) shouldBe Json.toJson(List(outputTY))
    }

    "calculate tax savings per claimant (claimant 1 = period 1 - 0 eligible months, period 2 - 0 eligible months) (claimant 2 = period 1 - 0 eligible months," +
      " period 2 - 1 eligible months) (taxablePay both 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-12-2016", formatter)
      val fromDate2 = LocalDate.parse("21-12-2016", formatter)
      val toDate2 = LocalDate.parse("06-04-2017", formatter)

      val inputClaimant = ESCClaimant(qualifying = false, isPartner = false,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = false,
        escStartDate = fromDate)
      val inputPartner = ESCClaimant(qualifying = false, isPartner = true,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = false,
        escStartDate = fromDate)

      val inputClaimant2 = ESCClaimant(qualifying = false, isPartner = false,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = false,
        escStartDate = fromDate)
      val inputPartner2 = ESCClaimant(qualifying = true, isPartner = true,
        eligibleMonthsInPeriod = 1, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = true,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputPartner), children = List(buildChild(childCareCost = 90)))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2, inputPartner2), children = List(buildChild(childCareCost = 90)))

      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = false, eligibleMonthsInTaxYear = 0,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = false, escAmount = 0.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 0.00, taxSaving = 0.00, niSaving = 0.00),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 0, niPaid = 0), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 0, niPaid = 0))

      val outputPartner = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = true, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 730.60, niPaid = 359.20))

      val outputTY=models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(totalSaving=37.8, taxSaving=36, niSaving=1.8), List(outputClaimant, outputPartner))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      resultTY shouldBe List(outputTY)
    }

    "calculate tax savings per claimant (claimant 1 = period 1 - 0 eligible months, period 2 - 1 eligible months) (claimant 2 = period 1 - 0 eligible months," +
      " period 2 - 0 eligible months) (taxablePay both 50000.00) (Monthly)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-12-2016", formatter)
      val fromDate2 = LocalDate.parse("21-12-2016", formatter)
      val toDate2 = LocalDate.parse("06-04-2017", formatter)

      val inputPartner = ESCClaimant(qualifying = false, isPartner = true,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = false,
        escStartDate = fromDate)
      val inputClaimant = ESCClaimant(qualifying = false, isPartner = false,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = false,
        escStartDate = fromDate)

      val inputPartner2 = ESCClaimant(qualifying = false, isPartner = true,
        eligibleMonthsInPeriod = 0, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = false,
        escStartDate = fromDate)
      val inputClaimant2 = ESCClaimant(qualifying = true, isPartner = false,
        eligibleMonthsInPeriod = 1, previousIncome = None, currentIncome = Some(ESCIncome(Some(50000))), vouchers = true,
        escStartDate = fromDate)

      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(inputClaimant, inputPartner), children = List(buildChild(childCareCost = 90)))
      val period2 = ESCPeriod(from = fromDate2, until = toDate2, claimants = List(inputClaimant2, inputPartner2), children = List(buildChild(childCareCost = 90)))

      val taxYear = ESCTaxYear(startDate = fromDate, endDate = toDate, periods = List(period, period2))

      val outputPartner = models.output.esc.ESCClaimant(qualifying = false, eligibleMonthsInTaxYear = 0,
        isPartner = true, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = false, escAmount = 0.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 0.00, taxSaving = 0.00, niSaving = 0.00),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 0, niPaid = 0), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 0, niPaid = 0))

      val outputClaimant = models.output.esc.ESCClaimant(qualifying = true, eligibleMonthsInTaxYear = 1,
        isPartner = false, income = models.output.esc.ESCIncome(taxablePay = 50000.00, gross = 50000.00, taxCode = "", niCategory = "A"),
        vouchers = true, escAmount = 90.0, escAmountPeriod = Periods.Monthly, escStartDate = fromDate,
        savings = ESCSavings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8),
        taxAndNIBeforeSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 766.60, niPaid = 361.00), taxAndNIAfterSacrifice = models.output.esc.ESCTaxAndNi(taxPaid = 730.60, niPaid = 359.20))

      val outputTY = models.output.esc.ESCTaxYear(fromDate, toDate, ESCSavings(totalSaving = 37.8, taxSaving = 36, niSaving = 1.8), List(outputClaimant, outputPartner))
      val resultTY = ESCCalculator.getCalculatedTaxYears(ESCCalculatorInput(List(taxYear), location))

      resultTY shouldBe List(outputTY)
    }

    // NI TESTS //
    "allocate gross earnings to NI bands (earnings in lower earnings level, TY2016, category A)" in {
      val grossPay = BigDecimal(485.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in primary earnings level, TY2016, category A)" in {
      val grossPay = BigDecimal(485.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 0.01, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in primary earnings level, TY2016, category B)" in {
      val grossPay = BigDecimal(672)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper accrual earnings level, TY2016, category B)" in {
      val grossPay = BigDecimal(672.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 0.01, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper accrual earnings level, TY2016, category C)" in {
      val grossPay = BigDecimal(3337.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2665, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper  earnings level, TY2016, category A)" in {
      val grossPay = BigDecimal(3337.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2665.01, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in upper  earnings level, TY2016, category C)" in {
      val grossPay = BigDecimal(3532.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in above upper earnings level, TY2016, category C)" in {
      val grossPay = BigDecimal(3532.01)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860.01, aboveUpperEarningsBand = 0.0)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period,  ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "allocate gross earnings to NI bands (earnings in above upper earnings level, TY2017, category B)" in {
      val grossPay = BigDecimal(10000.00)
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val outputModel = CalculationNIBands(lowerEarningsBand = 490.00, primaryEarningsBand = 190, upperEarningsBand = 3073, aboveUpperEarningsBand = 6247)
      val result = ESCCalculator.allocateAmountToNIBands(grossPay, period, ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 485 (lower earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 600 (primary earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 115, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 672 (primary earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period, ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2017, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 128, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 15.36, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period, ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 128, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 7.4880, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2016, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 128, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2828, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 339.36, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2828, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 165.438, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2016, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2828, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2680, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 321.60, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2016, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2680, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 156.78, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period, ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2016, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2680, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 485 (lower earnings level), TY2017, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 600 (primary earnings level), TY2017, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 115, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 672 (primary earnings level), TY2017, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 800 (upper accrual earnings level), TY2016, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 128, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 15.36, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 3500 (upper earnings level), TY2017, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2828, aboveUpperEarningsBand = 0)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 165.438, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2017, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2017", formatter)
      val toDate = LocalDate.parse("21-05-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())
      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2017, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("06-04-2017", formatter)
      val toDate = LocalDate.parse("05-04-2018", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())
      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 167.31, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2018, category A)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2018", formatter)
      val toDate = LocalDate.parse("05-04-2019", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())
      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 343.20, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "A", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2018, category B)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("06-04-2018", formatter)
      val toDate = LocalDate.parse("05-04-2019", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())
      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 167.31, aboveUpperEarningsBand = 9.36)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "B", location))

      result shouldBe outputModel
    }

    "determine NI amount for each band (earnings is 4000 (above upper earnings level), TY2018, category C)" in {
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2018", formatter)
      val toDate = LocalDate.parse("05-04-2019", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())
      val inputModel = CalculationNIBands(lowerEarningsBand = 485.00, primaryEarningsBand = 187, upperEarningsBand = 2860, aboveUpperEarningsBand = 468)
      val outputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val result = ESCCalculator.calculateNIPerBand(inputModel, period,  ESCConfig.getConfig(period.from, "C", location))

      result shouldBe outputModel
    }

    "determine total NI due (Income 4000 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 319.8, upperEarningsBand = 23.4, aboveUpperEarningsBand = 9.36)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(352.56)
    }

    "determine total NI due (Income 3500 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 155.9025, upperEarningsBand = 9.5355, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(165.44)
    }

    "determine total NI due (Income 800 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 15.36, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(15.36)
    }

    "determine total NI due (Income 672 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "determine total NI due (Income 600 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.0)
    }

    "determine total NI due (Income 485 monthly)" in {
      val inputModel = CalculationNIBands(lowerEarningsBand = 0, primaryEarningsBand = 0, upperEarningsBand = 0, aboveUpperEarningsBand = 0)
      val calcPeriod = Periods.Yearly

      val result = ESCCalculator.totalNIDue(inputModel, calcPeriod)
      result shouldBe BigDecimal(0.00)
    }

    "determine total NI savings per month after salary sacrifice for niCategory A (income 4000, voucher amount 200)" in {
      val grossPay = BigDecimal(4000.00)
      val reliefAmount = BigDecimal(124.00)
      val calcPeriod = Periods.Yearly
      val formatter = DateTimeFormat.forPattern("dd-MM-yyyy")
      val fromDate = LocalDate.parse("01-05-2016", formatter)
      val toDate = LocalDate.parse("21-05-2017", formatter)
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val result = ESCCalculator.calculateNISavings(period, grossPay, reliefAmount, ESCConfig.getConfig(period.from, "A", location), calcPeriod)
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
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val result = ESCCalculator.calculateNISavings(period, grossPay, reliefAmount, ESCConfig.getConfig(period.from, "B", location), calcPeriod)
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
      val period = ESCPeriod(from = fromDate, until = toDate, claimants = List(), children = List[Child]())

      val result = ESCCalculator.calculateNISavings(period, grossPay, reliefAmount, ESCConfig.getConfig(period.from, "A", location), calcPeriod)
      result._1 shouldBe BigDecimal(24)
      result._2 shouldBe BigDecimal(338.40)
      result._3 shouldBe BigDecimal(314.4)
    }

    "Generate total award with claimants (Total Award test)" in {
      val resource: JsonNode = JsonLoader.fromResource("/json/esc/input/calculator_input_test.json")
      val json: JsValue = Json.parse(resource.toString)
      val inputJson = json.validate[ESCCalculatorInput]
      inputJson.isInstanceOf[JsSuccess[ESCCalculatorInput]] shouldBe true

      val result: ESCCalculatorOutput = ESCCalculator.award(inputJson.get)

      val resourceJson = JsonLoader.fromResource("/json/esc/output/output_test_1.json")
      val outputJson: JsValue = Json.parse(resourceJson.toString)

      Json.toJson(result) shouldBe outputJson
    }

    "Validate tax code and return personal allowance (11000)" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "1100L", niCategory = "")
      ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location)) shouldBe 11000
    }

    "Validate tax code and return personal allowance (9999)" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "9999M", niCategory = "")
      ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location)) shouldBe 99990
    }

    "Validate tax code and return personal allowance (9Y)" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "9Y", niCategory = "")
      ESCCalculator.getPersonalAllowance(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location)) shouldBe 90
    }

    "Validate tax code and return D1" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "d1", niCategory = "")
      ESCCalculator.getTaxCode(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location)) shouldBe "D1"
    }

    "Return error for invalid tax code (Y)" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "Y", niCategory = "")
      try {
        val result = ESCCalculator.getTaxCode(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
        result shouldBe a[NoSuchElementException]
      } catch {
        case e: Exception =>
          e shouldBe a[NoSuchElementException]
      }
    }

    "return error for invalid tax code (D11)" in {
      val periodStart = LocalDate.parse("2016-05-06", formatter)
      val periodEnd = LocalDate.parse("2017-04-06", formatter)

      val period = ESCPeriod(from = periodStart, until = periodEnd, List(), children = List())
      val income = ESCTotalIncome(taxablePay = BigDecimal(0.00), gross = BigDecimal(0.00), taxCode = "D11", niCategory = "")
      try {
        val result = ESCCalculator.getTaxCode(period, income, ESCConfig.getConfig(period.from, income.niCategory.toUpperCase.trim, location))
        result shouldBe a[NoSuchElementException]
      } catch {
        case e: Exception =>
          e shouldBe a[NoSuchElementException]
      }
    }

  "calculatedNIAdditionalAllowance" should {
    val maxPossibleAllocationAtLowestBand = 243

    "nothing after tax allowance allowance" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 0,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = 100,
        allocatedReliefAmountLowestIncome = 0)

      result shouldBe ((0, 0))
    }
    "neither has spare max allowance" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 100,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountLowestIncome = maxPossibleAllocationAtLowestBand)

      result shouldBe ((0, 0))
    }

    "higher earner has spare max allowance" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 100,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = 100,
        allocatedReliefAmountLowestIncome = maxPossibleAllocationAtLowestBand)

      result shouldBe ((100, 0))
    }
    "lower earner has spare max allowance" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 100,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountLowestIncome = 0)

      result shouldBe ((0, 100))
    }
    "both has spare max allowance" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 100,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = 200,
        allocatedReliefAmountLowestIncome = 100)

      result shouldBe ((43, 57))
    }

    "lower earner has spare max allowance for Ni" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 107,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountLowestIncome = 0)

      result shouldBe ((0, 107))
    }

    "Scenario not possible with current usage but increases coverage" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 1000,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = 100,
        allocatedReliefAmountLowestIncome = 0)

      result shouldBe ((143, 243))
    }

    "Scenario not possible with current usage but increases coverage 2" in {
      val result = ESCCalculator.calculatedNIAdditionalAllowance(
        amountLeftAfterTaxAssignments = 1000,
        maxPossibleAllocationAtLowestBand = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountHighestIncome = maxPossibleAllocationAtLowestBand,
        allocatedReliefAmountLowestIncome = 0)

      result shouldBe ((0, 243))
    }
  }
  }
}
