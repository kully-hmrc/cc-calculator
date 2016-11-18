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

import com.typesafe.config.ConfigFactory
import controllers.FakeCCCalculatorApplication
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Configuration
import uk.gov.hmrc.play.test.UnitSpec
import utils.ESCConfig._

/**
 * Created by user on 25/01/16.
 */
class ESCSchemeConfigSpec extends UnitSpec with FakeCCCalculatorApplication with ESCConfig {
  "ESC SchemeConfig" should {

    "(ESC) populate upper months limit from config file" in {
      upperMonthsLimitValidation shouldBe 100
    }

    "(ESC) populate lower months limit from config file" in {
      lowerMonthsLimitValidation shouldBe 0
    }

    "(ESC) populate lower claimants limit from config file" in {
      lowerClaimantsLimitValidation shouldBe 1
    }

    "(ESC) populate lower periods limit from config file" in {
      lowerPeriodsLimitValidation shouldBe 1
    }

    "(ESC) populate lower tax years limit from config file" in {
      lowerTaxYearsLimitValidation shouldBe 1
    }

    "(ESC) populate maximum monthly exemption pre-2011 from config file" in {
      pre2011MaxExemptionMonthly shouldBe 243
    }


    "Return ESCTaxYearConfig when niCategoryCode is empty" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("23-05-2016", formatter)
      val config = ESCConfig.getConfig(fromDate,"")

      val niCat = NiCategory(
        niCategoryCode = "A",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 12.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 12.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear =  ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate = 0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear     
    }

    "Return error for invalid niCategoryCode" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("23-05-2016", formatter)
      try {
        val result = ESCConfig.getConfig(fromDate,"Z")
        result shouldBe a[NoSuchElementException]
      } catch {
        case e: Exception =>
          e shouldBe a[NoSuchElementException]
      }
    }

    "Return error if the configuration details are not present in the scheme config file for a valid niCategoryCode" in {
      val configuration = Configuration(ConfigFactory.load((new java.io.File("/testconfig-esc.conf").getName)))
      val configs: Seq[play.api.Configuration] = configuration.getConfigSeq("test-esc.rule-change").get
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("23-05-2017", formatter)
      // fetch the config if it matches the particular year
      val configForTaxYear = getConfigForTaxYear(fromDate, configs)
      try {
        val result = configForTaxYear match {
          case Some(x) =>
            getNiCategory("B", x)
          case _ => 0
        }
        result shouldBe a[NoSuchElementException]
      } catch {
        case e: Exception =>
          e shouldBe a[NoSuchElementException]
      }
    }
    "Default ESC SchemeConfig - NI cat A" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-05-2015", formatter)
      val config = ESCConfig.getConfig(now,"A")
      val niCat = NiCategory(
        niCategoryCode = "A",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 12.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 12.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear
    }
    "(2016) ESC SchemeConfig - NI cat A" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-05-2016", formatter)
      val config = ESCConfig.getConfig(now,"A")
      val niCat = NiCategory(
        niCategoryCode = "A",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 12.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 12.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear    
    }

    "(2016) ESC SchemeConfig - NI cat B" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-05-2016", formatter)
      val config = ESCConfig.getConfig(now,"B")
      val niCat = NiCategory(
        niCategoryCode = "B",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 5.85,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 5.85,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear
    }

    "(2017) ESC SchemeConfig - NI cat C" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-07-2017", formatter)
      val config = ESCConfig.getConfig(now,"C")
      val niCat = NiCategory(
        niCategoryCode = "C",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 0.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 0.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 0.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear
    }

    "(2017) ESC SchemeConfig - NI cat A" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-07-2017", formatter)
      val config = ESCConfig.getConfig(now,"A")
      val niCat = NiCategory(
        niCategoryCode = "A",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 12.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 12.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear
    }

    "(2017) ESC SchemeConfig - Default NI cat " in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-07-2017", formatter)
      val config = ESCConfig.getConfig(now, "")
      val niCat = NiCategory(
        niCategoryCode = "A",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 12.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 12.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear
    }

    "ESC SchemeConfig - 2017 Tax Year" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val now = LocalDate.parse("23-03-2018", formatter)
      val config = ESCConfig.getConfig(now,"A")
      val niCat = NiCategory(
        niCategoryCode = "A",
        lelMonthlyLowerLimitForCat = 0.00,
        lelMonthlyUpperLimitForCat = 485.00,
        lelRateForCat = 0.00,
        lelPtMonthlyLowerLimitForCat = 486.00,
        lelPtMonthlyUpperLimitForCat = 672.00,
        lelPtRateForCat = 0.00,
        ptUapMonthlyLowerLimitForCat = 673.00,
        ptUapMonthlyUpperLimitForCat = 3337.00,
        ptUapRateForCat = 12.00,
        uapUelMonthlyLowerLimitForCat = 3338.00,
        uapUelMonthlyUpperLimitForCat = 3583.00,
        uapUelRateForCat = 12.00,
        aboveUelMonthlyLowerLimitForCat = 3584.00,
        aboveUelRateForCat = 2.00
      )
      val taxYear = ESCTaxYearConfig(
        post2011MaxExemptionMonthlyBasic = 243.00,
        post2011MaxExemptionMonthlyHigher = 124.00,
        post2011MaxExemptionMonthlyAdditional = 110.00,
        defaultTaxCode = "1100L",
        personalAllowanceRate =0.00,
        defaultPersonalAllowance = 11000,
        taxBasicRate = 20.00,
        taxBasicBandCapacity = 32000.00,
        taxHigherRate = 40.00,
        taxHigherBandUpperLimit = 150000.00,
        taxAdditionalRate = 45.00,
        taxAdditionalBandLowerLimit= 150000.01,
        niCategory = niCat
      )
      config shouldBe taxYear
    }

    "accessing ptUapMonthlyUpperLimitForCat" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("06-07-2016", formatter)
      val escTaxYearConfig = ESCConfig.getConfig(fromDate,"A")
      escTaxYearConfig.niCategory.ptUapMonthlyUpperLimitForCat shouldBe 3337.00
    }

    "accessing taxBasicBandCapacity" in {
      val pattern = "dd-MM-yyyy"
      val formatter = DateTimeFormat.forPattern(pattern)
      val fromDate = LocalDate.parse("06-07-2016", formatter)
      val escTaxYearConfig = ESCConfig.getConfig(fromDate,"A")
      escTaxYearConfig.taxBasicBandCapacity shouldBe 32000.00
    }

  }
}