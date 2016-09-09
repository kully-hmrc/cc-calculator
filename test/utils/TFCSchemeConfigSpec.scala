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

package utils

import controllers.FakeCCCalculatorApplication
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.play.test.UnitSpec

/**
 * Created by shridhaarchippaa on 22/06/15.
 */

class TFCSchemeConfigSpec extends UnitSpec with FakeCCCalculatorApplication {

  "TFC SchemeConfig" should {

    "populate end day of the tax year from config file" in {
      TFCConfig.taxYearEndDay shouldBe 6
    }

    "populate end month of the tax year from config file" in {
      TFCConfig.taxYearEndMonth shouldBe 4
    }

    "return max no of children from config file" in {
      TFCConfig.maxNoOfChildren shouldBe 25
    }

    "return max name length from config file" in {
      TFCConfig.maxNameLength shouldBe 25
    }

    "(following year) return the end date of tax year" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-06-2016", formatter)
      val endOfTaxYear = TFCConfig.taxYearEndDate(now, "tfc")
      endOfTaxYear shouldBe LocalDate.parse("06-04-2017", formatter)
    }

    "(current year) return the end date of tax year" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-02-2016", formatter)
      val endOfTaxYear = TFCConfig.taxYearEndDate(now, "tfc")
      endOfTaxYear shouldBe LocalDate.parse("06-04-2016", formatter)
    }

    "return 2016/2017 TFC taxYear Config for a date 24-07-2016" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("24-07-2016", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2017/2018 TFC taxYear Config for a date 24-07-2017" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("24-07-2017", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2017/2018 TFC taxYear Config for a date 24-07-2018" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("24-07-2018", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return default TFC taxYear Config for a date 24-07-2015" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("24-07-2015", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return default TFC taxYear Config for a date 05-04-2016" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("05-04-2016", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2016/2017 TFC taxYear Config for a date 06-04-2016" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("06-04-2016", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2016/2017 TFC taxYear Config for a date 05-04-2017" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("05-04-2017", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2017/2018 TFC taxYear Config for a date 06-04-2017" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("06-04-2017", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2017/2018 TFC taxYear Config for a date 05-04-2018" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("05-04-2018", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

    "return 2017/2018 TFC taxYear Config for a date 06-04-2018" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("06-04-2018", formatter)
      val config = TFCConfig.getConfig(fromDate)
      val tfcTaxYear = TFCTaxYearConfig(
        topUpPercent = 20,
        maxEligibleChildcareAmount = 2500,
        maxEligibleChildcareAmountForDisabled = 5000,
        maxGovtContribution = 500,
        maxGovtContributionForDisabled = 1000
      )
      config shouldBe tfcTaxYear
    }

  }

}
