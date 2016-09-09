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

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Play._
import play.api.{Logger, Configuration, Play}
import uk.gov.hmrc.play.config.ServicesConfig

/**
 * Created by user on 27/01/16.
 */
trait TCConfig {
  lazy val maxNameLength = configuration.getInt(s"tc.max-name-length").getOrElse(25)
  lazy val taxYearEndMonth = configuration.getInt(s"tc.end-of-tax-year-date.month").getOrElse(0)
  lazy val taxYearEndDay = configuration.getInt(s"tc.end-of-tax-year-date.day").getOrElse(0)
}
case class WTC(
                basicElement : Int,
                coupleElement : Int,
                loneParentElement: Int,
                hours30Element: Int,
                disabledWorkerElement: Int,
                severeDisabilityWorkerElement: Int,
                maxChildcareOneChildElement: Int,
                maxChildcareMoreChildrenElement: Int,
                eligibleCostCoveredPercent: Int
                )

case class CTC(
                childElement: Int,
                youngPersonElement: Int,
                disabledChildElement: Int,
                severeDisabilityChildElement: Int,
                familyElement: Int
                )

case class Thresholds(
                       wtcIncomeThreshold: Int,
                       ctcIncomeThreshold: Int,
                       taperRatePercent: Int
                       )

case class TCTaxYearConfig(
                            wtc: WTC,
                            ctc: CTC,
                            thresholds: Thresholds
                            )
object TCConfig extends CCConfig with TCConfig with ServicesConfig {

  def getCurrentTaxYearDateRange(fromDate : LocalDate) : (LocalDate, LocalDate) = {
    Logger.debug(s"TCConfig.getCurrentTaxYearDateRange - Begin")
    val pattern = "dd-MM-yyyy"
    val formatter = DateTimeFormat.forPattern(pattern)
    val month = TCConfig.taxYearEndMonth
    val day = TCConfig.taxYearEndDay
    val currentTaxYear = getCurrentTaxYear(fromDate)

    val taxYearStartDate = LocalDate.parse(s"$day-$month-$currentTaxYear", formatter)
    val taxYearEndDate = LocalDate.parse(s"$day-$month-${currentTaxYear + 1}", formatter)
    Logger.debug(s"TCConfig.getCurrentTaxYearDateRange - End")
    (taxYearStartDate, taxYearEndDate)
  }

  def getConfig(currentDate: LocalDate) : TCTaxYearConfig  = {
    Logger.debug(s"TCConfig.getConfig - Begin")

    val configs: Seq[play.api.Configuration] = Play.application.configuration.getConfigSeq("tc.rule-change").get
    // get the default config and keep
    val defaultConfig = configs.filter(x => {
      x.getString("rule-date").equals(Some("default"))
    }).head
    // fetch the config if it matches the particular year
    val result = getConfigForTaxYear(currentDate, configs)

    val config : TCTaxYearConfig = result match {
      case Some(x) =>
        getTaxYear(x)
      case _ =>
        getTaxYear(defaultConfig)
    }
    Logger.debug(s"TCConfig.getConfig - End")
    config
  }

  def getTaxYear(config : Configuration): TCTaxYearConfig = {
    Logger.debug(s"TCConfig.getTaxYear")
    TCTaxYearConfig(
      wtc = WTC(
        basicElement = config.getInt("input-elements.wtc.basic-element").get,
        coupleElement = config.getInt("input-elements.wtc.second-adult-element").get,
        loneParentElement = config.getInt("input-elements.wtc.lone-parent-element").get,
        hours30Element = config.getInt("input-elements.wtc.30-hour-element").get,
        disabledWorkerElement = config.getInt("input-elements.wtc.disabled-worker-element").get,
        severeDisabilityWorkerElement = config.getInt("input-elements.wtc.severe-disabled-worker-element").get,
        maxChildcareOneChildElement = config.getInt("input-elements.wtc.max-childcare-element-one-child").get,
        maxChildcareMoreChildrenElement = config.getInt("input-elements.wtc.max-childcare-element-more-children").get,
        eligibleCostCoveredPercent = config.getInt("input-elements.wtc.percent-of-eligible-cost-covered").get
      ),
      ctc = CTC(
        childElement = config.getInt("input-elements.ctc.child-element").get,
        youngPersonElement = config.getInt("input-elements.ctc.young-person-element").get,
        disabledChildElement = config.getInt("input-elements.ctc.disabled-child-element").get,
        severeDisabilityChildElement = config.getInt("input-elements.ctc.severe-disabled-child-element").get,
        familyElement = config.getInt("input-elements.ctc.family-element").get
      ),
      thresholds = Thresholds(
        wtcIncomeThreshold = config.getInt("thresholds.wtc-income-threshold").get,
        ctcIncomeThreshold = config.getInt("thresholds.ctc-income-threshold").get,
        taperRatePercent = config.getInt("thresholds.percent-of-taper-rate").get
      )
    )
  }
}
