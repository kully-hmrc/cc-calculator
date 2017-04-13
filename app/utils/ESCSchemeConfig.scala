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

package utils

import org.joda.time.LocalDate
import play.api.Play._
import play.api.Play.current
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.ServicesConfig

/**
 * Created by user on 22/01/16.
 */
trait ESCConfig extends ServicesConfig with LoadConfig {
  lazy val upperMonthsLimitValidation = getInt(s"esc.months-upper-limit")
  lazy val lowerMonthsLimitValidation = getInt(s"esc.months-lower-limit")
  lazy val lowerPeriodsLimitValidation = getInt(s"esc.periods-lower-limit")
  lazy val lowerTaxYearsLimitValidation = getInt(s"esc.tax-years-lower-limit")
  lazy val lowerClaimantsLimitValidation = getInt(s"esc.claimants-lower-limit")
  lazy val pre2011MaxExemptionMonthly = conf.getDouble(s"esc.pre-2011-maximum-exemption.basic-higher-additional.monthly").getOrElse(0.00)
}

case class NiCategory(
          niCategoryCode : String,
          lelMonthlyLowerLimitForCat : Double,
          lelMonthlyUpperLimitForCat: Double,
          lelRateForCat : Double,
          lelPtMonthlyLowerLimitForCat : Double,
          lelPtMonthlyUpperLimitForCat: Double,
          lelPtRateForCat: Double,
          ptUelMonthlyLowerLimitForCat: Double,
          ptUelMonthlyUpperLimitForCat: Double,
          ptUelRateForCat: Double,
          aboveUelMonthlyLowerLimitForCat: Double,
          aboveUelRateForCat: Double
                       )
case class ESCTaxYearConfig (
                             post2011MaxExemptionMonthlyBasic : Double,
                             post2011MaxExemptionMonthlyHigher : Double,
                             post2011MaxExemptionMonthlyAdditional : Double,
                             defaultTaxCode: String,
                             personalAllowanceRate: Double,
                             defaultPersonalAllowance: Double,
                             taxBasicRate: Double ,
                             taxBasicBandCapacity: Double ,
                             taxHigherRate: Double,
                             taxHigherBandUpperLimit: Double,
                             taxAdditionalRate: Double,
                             taxAdditionalBandLowerLimit: Double,
                             niCategory: NiCategory
                             )

object ESCConfig extends CCConfig with ServicesConfig with MessagesObject with LoadConfig {

  def getConfig(currentDate: LocalDate, niCategoryCode : String): ESCTaxYearConfig = {
    val configs: Seq[play.api.Configuration] = conf.getConfigSeq("esc.rule-change").get

    // get the default config and keep
    val defaultConfig = configs.filter(x => {
          x.getString("rule-date").equals(Some("default"))
        }).head
    // fetch the config if it matches the particular year
    val result = getConfigForTaxYear(currentDate, configs)

    val config : ESCTaxYearConfig = result match {
      case Some(x) => getTaxYear(niCategoryCode, x)
      case _ => getTaxYear(niCategoryCode, defaultConfig)
    }
    config
  }

  def getTaxYear(niCategoryCode : String, config : Configuration): ESCTaxYearConfig = {
    // get the ni Category
    val niCat = getNiCategory(niCategoryCode, config)
    ESCTaxYearConfig(
      post2011MaxExemptionMonthlyBasic = config.getDouble("post-2011-maximum-exemption.basic.monthly").get,
      post2011MaxExemptionMonthlyHigher = config.getDouble("post-2011-maximum-exemption.higher.monthly").get,
      post2011MaxExemptionMonthlyAdditional = config.getDouble("post-2011-maximum-exemption.additional.monthly").get,
      defaultTaxCode = config.getString("tax.default-tax-code").get,
      personalAllowanceRate = config.getDouble("tax.personal-allowance.rate").get,
      defaultPersonalAllowance = config.getDouble("tax.personal-allowance.default-personal-allowance").get,
      taxBasicRate = config.getDouble("tax.basic.rate").get,
      taxBasicBandCapacity = config.getDouble("tax.basic.band-capacity-annual-amount").get,
      taxHigherRate = config.getDouble("tax.higher.rate").get,
      taxHigherBandUpperLimit = config.getDouble("tax.higher.band-annual-upper-limit").get,
      taxAdditionalRate = config.getDouble("tax.additional.rate").get,
      taxAdditionalBandLowerLimit= config.getDouble("tax.additional.band-annual-lower-limit").get,
      niCategory = niCat
    )
  }

  def getNiCategory(niCategoryCode: String, config: play.api.Configuration) :NiCategory ={
    // get the ni Category
    val niCode = niCategoryCode match {
      case cat if cat.isEmpty => config.getString("default-ni-code").get
      case cat if cat.equals("A") || cat.equals("B") || cat.equals("C") => cat
      case _ => throw new NoSuchElementException(messages("cc.scheme.config.invalid.ni.category"))
    }
//    val output = getNiCategoryHelper(niCode,config.getConfigSeq("niCategories").get , None)
    val niCat  = getNiCategoryHelper(niCode, config.getConfigSeq("niCategories").get , None)
    match {
      case Some(z) => z
      case _ =>   throw new NoSuchElementException(messages("cc.scheme.config.ni.category.not.found"))
    }
    niCat
  }

  def getNiCategoryHelper(code: String, niCategories: Seq[play.api.Configuration], acc: Option[NiCategory]): Option[NiCategory] = {
    niCategories match {
      case Nil =>  acc
      case head :: tail =>
        if (head.getString("ni-cat-code").get.equals(code)) {
          val niCat = NiCategory(
            niCategoryCode = code,
            lelMonthlyLowerLimitForCat = head.getDouble("LEL.monthly-lower-limit").get,
            lelMonthlyUpperLimitForCat = head.getDouble("LEL.monthly-upper-limit").get,
            lelRateForCat = head.getDouble("LEL.rate").get,
            lelPtMonthlyLowerLimitForCat = head.getDouble("LEL-PT.monthly-lower-limit").get,
            lelPtMonthlyUpperLimitForCat = head.getDouble("LEL-PT.monthly-upper-limit").get,
            lelPtRateForCat = head.getDouble("LEL-PT.rate").get,
            ptUelMonthlyLowerLimitForCat = head.getDouble("PT-UEL.monthly-lower-limit").get,
            ptUelMonthlyUpperLimitForCat = head.getDouble("PT-UEL.monthly-upper-limit").get,
            ptUelRateForCat = head.getDouble("PT-UEL.rate").get,
            aboveUelMonthlyLowerLimitForCat = head.getDouble("above-UEL.monthly-lower-limit").get,
            aboveUelRateForCat = head.getDouble("above-UEL.rate").get
          )
          getNiCategoryHelper(code, Nil, Some(niCat))
        }
        else {
          getNiCategoryHelper(code, tail, acc)
        }
    }
  }
}
