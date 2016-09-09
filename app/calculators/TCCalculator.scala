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

import models.input.APIModels.Request
import models.input.tc._
import models.output.OutputAPIModel.AwardPeriod
import models.output.tc.{Element, Elements, TCCalculation, TaxYear}
import org.joda.time.LocalDate
import play.api.Logger
import utils.{Periods, TCConfig}
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode
import scala.util.{Failure, Success}

/**
 * Created by adamconder on 08/06/15.
 */

object TCCalculator extends TCCalculator

trait TCCalculator extends CCCalculator {

  val calculator = new TCCalculatorService

  trait TCCalculatorTapering {
    this: TCCalculatorService =>

    protected def getPeriodAmount(period: models.output.tc.Period, amount : BigDecimal = 0.00, fullCalculationRequired: Boolean) : models.output.tc.Period = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.getPeriodAmount")
      models.output.tc.Period(
        from = period.from,
        until = period.until,
        elements = Elements(
          wtcWorkElement = Element(
            maximumAmount = period.elements.wtcWorkElement.maximumAmount,
            netAmount = period.elements.wtcWorkElement.maximumAmount,
            taperAmount = BigDecimal(0.00)
          ),
          wtcChildcareElement = Element(
            maximumAmount = period.elements.wtcChildcareElement.maximumAmount,
            netAmount = period.elements.wtcChildcareElement.maximumAmount,
            taperAmount = BigDecimal(0.00)
          ),
          ctcIndividualElement = Element(
            maximumAmount = period.elements.ctcIndividualElement.maximumAmount,
            netAmount = period.elements.ctcIndividualElement.maximumAmount,
            taperAmount = BigDecimal(0.00)
          ),
          ctcFamilyElement = Element(
            maximumAmount = period.elements.ctcFamilyElement.maximumAmount,
            netAmount = period.elements.ctcFamilyElement.maximumAmount,
            taperAmount = BigDecimal(0.00)
          )
        ),
        periodNetAmount = {
          if(fullCalculationRequired) amount else BigDecimal(0.00)
        },
        periodAdviceAmount = {
          if(fullCalculationRequired) BigDecimal(0.00) else amount
        }
      )

    }

    def netAmountPerElementPerPeriod(taperAmount: BigDecimal, maximumAmountPerElement: BigDecimal) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.netAmountPerElementPerPeriod")
      if (taperAmount.>(maximumAmountPerElement)) {
        //WTC work element is nil and further taper is required
        BigDecimal(0.00)
      } else {
        //tapering stops send the difference in amount as WTC element
        maximumAmountPerElement - taperAmount
      }
    }

    def isTaperingRequiredForElements(income: BigDecimal, threshold: BigDecimal) : Boolean = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.isTaperingRequiredForElements")
      income > threshold
    }

    def getHigherAmount(amount1: BigDecimal, amount2: BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.getHigherAmount")
      if (amount1.>=(amount2))
        amount1
      else
        amount2
    }

    //In order to maintain consistency across TC logic the amount is truncated to 3 digits after decimal point and rounded up to the nearest pence
    def earningsAmountToTaperForPeriod(income: BigDecimal, thresholdIncome: BigDecimal, period: Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.earningsAmountToTaperForPeriod")
      val taperRate = period.config.thresholds.taperRatePercent
      val rawResult = (income - thresholdIncome) * (taperRate / BigDecimal(100.00))
      round(roundDownToThreeDigits(rawResult))
    }

    def getPercentOfAmount(amount : BigDecimal, percentage : Int) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.getPercentOfAmount")
      (amount / 100) * percentage
    }

    def wtcIncomeThresholdForPeriod(period : Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.wtcIncomeThresholdForPeriod - Begin")
      val thresholdConfig = period.config.thresholds.wtcIncomeThreshold
      val thresholdForPeriod = amountForDateRange(thresholdConfig, Periods.Yearly, period.from, period.until)
      Logger.debug(s"TCCalculator.TCCalculatorTapering.wtcIncomeThresholdForPeriod - End")
      thresholdForPeriod
    }

    def ctcIncomeThresholdForPeriod(period : Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.ctcIncomeThresholdForPeriod - Begin")
      val thresholdConfig = period.config.thresholds.ctcIncomeThreshold
      val thresholdForPeriod = amountForDateRange(thresholdConfig, Periods.Yearly, period.from, period.until)
      Logger.debug(s"TCCalculator.TCCalculatorTapering.ctcIncomeThresholdForPeriod - End")
      thresholdForPeriod
    }

    def incomeForPeriod(previousHouseHoldIncome : BigDecimal, period : Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.incomeForPeriod - Begin")
      val incomeForPeriod = amountForDateRange(previousHouseHoldIncome, Periods.Yearly, period.from, period.until)
      Logger.debug(s"TCCalculator.TCCalculatorTapering.incomeForPeriod - End")
      incomeForPeriod
    }

    def taperFirstElement(period: models.output.tc.Period, inputPeriod: models.input.tc.Period, income: BigDecimal, wtcIncomeThreshold: BigDecimal): models.output.tc.Period = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.taperFirstElement")
      val wtcWorkElementMaxAmount = period.elements.wtcWorkElement.maximumAmount
      val firstTaperThreshold = earningsAmountToTaperForPeriod(income, wtcIncomeThreshold, inputPeriod)
      val wtcWorkNetAmount = netAmountPerElementPerPeriod(firstTaperThreshold, wtcWorkElementMaxAmount)

      models.output.tc.Period(
        from = period.from,
        until = period.until,
        elements = Elements(
          wtcWorkElement = Element(
            maximumAmount = wtcWorkElementMaxAmount,
            netAmount = wtcWorkNetAmount,
            taperAmount = {
              if (wtcWorkNetAmount.equals(BigDecimal(0.00))) {
                // cannot taper more than maximum amount for current element
                period.elements.wtcWorkElement.maximumAmount
              } else {
                firstTaperThreshold
              }
            }
          ),
          wtcChildcareElement = period.elements.wtcChildcareElement,
          ctcIndividualElement = period.elements.ctcIndividualElement,
          ctcFamilyElement = period.elements.ctcFamilyElement
        )
      )
    }

    def taperSecondElement(period: models.output.tc.Period, inputPeriod: models.input.tc.Period, income: BigDecimal, wtcIncomeThreshold: BigDecimal): models.output.tc.Period = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.taperSecondElement")
      val secondTaperAmount = earningsAmountToTaperForPeriod(income, wtcIncomeThreshold, inputPeriod)
      val secondTaperThreshold = secondTaperAmount - period.elements.wtcWorkElement.maximumAmount
      val wtcChildcareNetAmount = netAmountPerElementPerPeriod(secondTaperThreshold, period.elements.wtcChildcareElement.maximumAmount)
      models.output.tc.Period(
        from = period.from,
        until = period.until,
        elements = Elements(
          wtcWorkElement = period.elements.wtcWorkElement,
          wtcChildcareElement = Element(
            maximumAmount = period.elements.wtcChildcareElement.maximumAmount,
            netAmount = {
              if (period.elements.wtcWorkElement.netAmount.equals(BigDecimal(0.00))) {
                wtcChildcareNetAmount
              } else {
                period.elements.wtcChildcareElement.maximumAmount
              }
            },
            taperAmount = {
              if (period.elements.wtcWorkElement.netAmount.equals(BigDecimal(0.00))) {
                if (wtcChildcareNetAmount.equals(BigDecimal(0.00))) {
                  // cannot taper more than maximum amount for current element
                  period.elements.wtcChildcareElement.maximumAmount
                } else {
                  secondTaperThreshold
                }
              } else {
                BigDecimal(0.00)
              }
            }
          ),
          ctcIndividualElement = period.elements.ctcIndividualElement,
          ctcFamilyElement = period.elements.ctcFamilyElement
        )
      )
    }

    def taperThirdElement(period: models.output.tc.Period, inputPeriod: models.input.tc.Period, income: BigDecimal, wtcIncomeThreshold: BigDecimal, ctcIncomeThreshold: BigDecimal): (models.output.tc.Period, Boolean) = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.taperThirdElement")
      val taperRate = inputPeriod.config.thresholds.taperRatePercent
      val calculatedCTCThreshold = ((period.elements.wtcWorkElement.maximumAmount + period.elements.wtcChildcareElement.maximumAmount) / taperRate * 100) + wtcIncomeThreshold
      val roundedCalculatedCTCThreshold = round(roundDownToThreeDigits(calculatedCTCThreshold))
      val taperingThreshold = getHigherAmount(ctcIncomeThreshold, roundedCalculatedCTCThreshold)

      if (period.elements.wtcChildcareElement.netAmount.equals(BigDecimal(0.00))) {
        val outputPeriod = models.output.tc.Period(
          from = period.from,
          until = period.until,
          elements = Elements(
            wtcWorkElement = period.elements.wtcWorkElement,
            wtcChildcareElement = period.elements.wtcChildcareElement,
            ctcIndividualElement = Element(
              maximumAmount = period.elements.ctcIndividualElement.maximumAmount,
              netAmount = {
                if (income.>(taperingThreshold)) {
                  //further tapering required as income is too high
                  val thirdTaperAmount = earningsAmountToTaperForPeriod(income, taperingThreshold, inputPeriod)
                  netAmountPerElementPerPeriod(thirdTaperAmount, period.elements.ctcIndividualElement.maximumAmount)
                } else {
                  //income is lower than threshold so don't taper
                  period.elements.ctcIndividualElement.maximumAmount
                }
              },
              taperAmount = {
                if (income > taperingThreshold) {
                  //further tapering required as income is too high
                  val thirdTaperAmount = earningsAmountToTaperForPeriod(income, taperingThreshold, inputPeriod)
                  val netAmount = netAmountPerElementPerPeriod(thirdTaperAmount, period.elements.ctcIndividualElement.maximumAmount)
                  if (netAmount.equals(BigDecimal(0.00))) {
                    period.elements.ctcIndividualElement.maximumAmount
                  }
                  else {
                    thirdTaperAmount
                  }
                } else {
                  //pass maximum amount if taperAmount needs to be used in 4th tapering
                  BigDecimal(0.00)
                }
              }
            ),
            ctcFamilyElement = period.elements.ctcFamilyElement
          )
        )
        (outputPeriod, true)
      } else {
        //tapering not required
        val outputPeriod = models.output.tc.Period(
          from = period.from,
          until = period.until,
          elements = Elements(
            wtcWorkElement = period.elements.wtcWorkElement,
            wtcChildcareElement = period.elements.wtcChildcareElement,
            ctcIndividualElement = Element(
              maximumAmount = period.elements.ctcIndividualElement.maximumAmount,
              netAmount = period.elements.ctcIndividualElement.maximumAmount,
              taperAmount = BigDecimal(0.00)
            ),
            ctcFamilyElement = period.elements.ctcFamilyElement
          )
        )
        (outputPeriod, false)
      }
    }

    def taperFourthElement(period: models.output.tc.Period, inputPeriod: models.input.tc.Period, income: BigDecimal, wtcIncomeThreshold: BigDecimal, ctcIncomeThreshold: BigDecimal, continue: Boolean = false): models.output.tc.Period = {
      Logger.debug(s"TCCalculator.TCCalculatorTapering.taperFourthElement")
      if (continue) {
        val taperRate = inputPeriod.config.thresholds.taperRatePercent
        val incomeToTaperElementsNil = (period.elements.wtcWorkElement.maximumAmount + period.elements.wtcChildcareElement.maximumAmount + period.elements.ctcIndividualElement.maximumAmount) / taperRate * 100 + wtcIncomeThreshold
        val roundedIncomeToTaperElementsNil = round(roundDownToThreeDigits(incomeToTaperElementsNil))
        val taperingThreshold = getHigherAmount(ctcIncomeThreshold, roundedIncomeToTaperElementsNil)
        models.output.tc.Period(
          from = period.from,
          until = period.until,
          elements = Elements(
            wtcWorkElement = period.elements.wtcWorkElement,
            wtcChildcareElement = period.elements.wtcChildcareElement,
            ctcIndividualElement = period.elements.ctcIndividualElement,
            ctcFamilyElement = Element(
              maximumAmount = period.elements.ctcFamilyElement.maximumAmount,
              netAmount = {
                if (income.>(taperingThreshold)) {
                  val fourthTaperAmount = earningsAmountToTaperForPeriod(income, taperingThreshold, inputPeriod)
                  netAmountPerElementPerPeriod(fourthTaperAmount, period.elements.ctcFamilyElement.maximumAmount)
                } else {
                  period.elements.ctcFamilyElement.maximumAmount
                }
              },
              taperAmount = {
                if (income.>(taperingThreshold)) {
                  val fourthTaperAmount = earningsAmountToTaperForPeriod(income, taperingThreshold, inputPeriod)
                  val netAmount = netAmountPerElementPerPeriod(fourthTaperAmount, period.elements.ctcFamilyElement.maximumAmount)
                  if (netAmount.equals(BigDecimal(0.00))) {
                    period.elements.ctcFamilyElement.maximumAmount
                  }
                  else {
                    fourthTaperAmount
                  }
                } else {
                  BigDecimal(0.00)
                }
              }
            )
          )
        )
      } else {
        models.output.tc.Period(
          from = period.from,
          until = period.until,
          elements = Elements(
            wtcWorkElement = period.elements.wtcWorkElement,
            wtcChildcareElement = period.elements.wtcChildcareElement,
            ctcIndividualElement = period.elements.ctcIndividualElement,
            ctcFamilyElement = Element(
              netAmount = period.elements.ctcFamilyElement.maximumAmount,
              maximumAmount = period.elements.ctcFamilyElement.maximumAmount,
              taperAmount = BigDecimal(0.00)
            )
          )
        )
      }
    }

  }

  trait TCCalculatorElements {
    this : TCCalculatorService  =>

    def basicElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.basicElementForPeriod")
      if (period.householdElements.basic) {
        val basicElement = period.config.wtc.basicElement
        val basicElementForPeriod = amountForDateRange(basicElement, Periods.Yearly, period.from, period.until)
        (period.householdElements.basic, basicElementForPeriod)
      } else {
        (period.householdElements.basic, BigDecimal(0.00))
      }
    }

    def hours30ElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.hours30ElementForPeriod")
      if (period.householdElements.hours30) {
        val hours30Element = period.config.wtc.hours30Element
        val hours30ElementForPeriod = amountForDateRange(hours30Element, Periods.Yearly, period.from, period.until)
        (period.householdElements.hours30, hours30ElementForPeriod)
      } else {
        (period.householdElements.hours30, BigDecimal(0.00))
      }
    }

    def loneParentElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.loneParentElementForPeriod")
      if (period.householdElements.loneParent) {
        val loneParentElementMaximumAmount = period.config.wtc.loneParentElement
        val maximumAmountForDays = amountForDateRange(loneParentElementMaximumAmount, Periods.Yearly, period.from, period.until)
        (period.householdElements.loneParent, maximumAmountForDays)
      } else {
        (period.householdElements.loneParent, BigDecimal(0.00))
      }
    }

    def secondAdultElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.secondAdultElementForPeriod")
      if (period.householdElements.secondParent) {
        val coupleElementMaximumAmount = period.config.wtc.coupleElement
        val maximumAmountForDays = amountForDateRange(coupleElementMaximumAmount, Periods.Yearly, period.from, period.until)
        (period.householdElements.secondParent, maximumAmountForDays)
      } else {
        (period.householdElements.secondParent, BigDecimal(0.00))
      }
    }

    def maxFamilyElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.maxFamilyElementForPeriod")
      if (period.householdElements.family) {
        val familyElementMaximumAmount = period.config.ctc.familyElement
        val maximumAmountForDays = amountForDateRange(familyElementMaximumAmount, Periods.Yearly, period.from, period.until)
        (period.householdElements.family, maximumAmountForDays)
      } else {
        (period.householdElements.family, BigDecimal(0.00))
      }
    }

    def disabledWorkerElementForPeriod(period: Period, claimant: Claimant): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.disabledWorkerElementForPeriod")
      val element = claimant.getsDisabilityElement
      if (element) {
        val disabledWorkerElement = period.config.wtc.disabledWorkerElement
        val maximumAmountForDays = amountForDateRange(disabledWorkerElement, Periods.Yearly, period.from, period.until)
        (element, maximumAmountForDays)
      } else {
        (element, BigDecimal(0.00))
      }
    }

    def severelyDisabledWorkerElementForPeriod(period: Period, claimant: Claimant): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.severelyDisabledWorkerElementForPeriod")
      val element = claimant.getsSevereDisabilityElement
      if (element) {
        val severeDisabilityWorkerElement = period.config.wtc.severeDisabilityWorkerElement
        val maximumAmountForDays = amountForDateRange(severeDisabilityWorkerElement, Periods.Yearly, period.from, period.until)
        (element, maximumAmountForDays)
      } else {
        (element, BigDecimal(0.00))
      }
    }

    def childOrYoungAdultBasicElementForPeriod(period: Period, child: Child): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.childOrYoungAdultBasicElementForPeriod")
      val element = child.isQualifyingCTC
      if (element) {
        val childElementMaximumAmount = period.config.ctc.childElement
        val maximumAmountForDays = amountForDateRange(childElementMaximumAmount, Periods.Yearly, period.from, period.until)
        (element, maximumAmountForDays)
      } else {
        (element, BigDecimal(0.00))
      }
    }

    def childOrYoungAdultDisabilityElementForPeriod(period: Period, child: Child): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.childOrYoungAdultDisabilityElementForPeriod")
      val element = child.getsDisabilityElement
      if (element) {
        val maximumAmount = period.config.ctc.disabledChildElement
        val maximumAmountForDays = amountForDateRange(maximumAmount, Periods.Yearly, period.from, period.until)
        (element, maximumAmountForDays)
      } else {
        (element, BigDecimal(0.00))
      }
    }

    def childOrYoungAdultSevereDisabilityElementForPeriod(period: Period, child: Child): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.childOrYoungAdultSevereDisabilityElementForPeriod")
      val element = child.getsSevereDisabilityElement
      if (element) {
        val maximumAmount = period.config.ctc.severeDisabilityChildElement
        val maximumAmountForDays = amountForDateRange(maximumAmount, Periods.Yearly, period.from, period.until)
        (element, maximumAmountForDays)
      } else {
        (element, BigDecimal(0.00))
      }
    }

    def maxChildElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.maxChildElementForPeriod")
      period.children match {
        case head :: tail =>
          // get an amount List[(Boolean, BigDecimal)] for each child (including disability and severe if it applies)
          val elements = for (child <- period.children) yield {
            val basic = childOrYoungAdultBasicElementForPeriod(period, child)
            val disabled = childOrYoungAdultDisabilityElementForPeriod(period, child)
            val severeDisabled = childOrYoungAdultSevereDisabilityElementForPeriod(period, child)
            val amount = basic._2 + disabled._2 + severeDisabled._2
            (child.isQualifyingCTC, amount)
          }
          val amount = elements.foldLeft(BigDecimal(0.00))((acc, element) => acc + element._2)
          val numberOfQualifyingChildren = elements.foldLeft(0)((acc, element) => if (element._1) acc + 1 else acc)
          val hasQualifyingChildren = numberOfQualifyingChildren > 0
          (hasQualifyingChildren, amount)
        case Nil => (false, BigDecimal(0.00))
      }
    }

    private def claimantWorkElementsForPeriod(houseHoldElement: HouseHoldElements): Boolean = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.claimantWorkElementsForPeriod")
      val loneOrSecond = (houseHoldElement.basic, houseHoldElement.hours30, houseHoldElement.loneParent, houseHoldElement.secondParent)
      loneOrSecond match {
        case (hh, ho, false, true) =>
          // second
          true
        case (hh, ho, true, false) =>
          // lone
          true
        case _ => false
      }
    }

    def maxWorkElementForPeriod(period: Period): (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.maxWorkElementForPeriod")
      val basic = basicElementForPeriod(period)
      val hours30 = hours30ElementForPeriod(period)
      val loneParent = loneParentElementForPeriod(period)
      val secondAdult = secondAdultElementForPeriod(period)
      val houseHoldAmt = basic._2 + hours30._2 + loneParent._2 + secondAdult._2
      // get an amount List[(Boolean, BigDecimal)] for each claimant (including disability and severe if it applies)
      val elements = for (claimant <- period.claimants) yield {
        val disabled = disabledWorkerElementForPeriod(period, claimant)
        val severeDisabled = severelyDisabledWorkerElementForPeriod(period, claimant)
        val amount = disabled._2 + severeDisabled._2
        (claimantWorkElementsForPeriod(period.householdElements), amount)
      }
      val amount = elements.foldLeft(BigDecimal(0.00))((acc, element) => acc + element._2)
      val totalWTC = amount + houseHoldAmt
      val numberOfQualifyingClaimant = elements.foldLeft(0)((acc, element) => if (element._1) acc + 1 else acc)
      val hasQualifyingClaimant = numberOfQualifyingClaimant > 0
      (hasQualifyingClaimant, totalWTC)
    }

    protected def getChildcareThresholdPerWeek(period: models.input.tc.Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.getChildcareThresholdPerWeek")
      if (period.children.length > 1) {
        val threshold = period.config.wtc.maxChildcareMoreChildrenElement
        threshold
      } else if(period.children.length == 1) {
        val threshold = period.config.wtc.maxChildcareOneChildElement
        threshold
      } else {
        BigDecimal(0.00)
      }
    }

    private def getTotalChildcarePerWeek(period: Period) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.getTotalChildcarePerWeek - Begin")
      val amountPerWeek = period.children.foldLeft(BigDecimal(0.00)) {
        (acc, child) =>
          if (child.isQualifyingWTC) {
            // return cost per week
            val annual = amountToAnnualAmount(child.childcareCost, child.childcareCostPeriod)
            val amountPerWeek = amountToWeeklyAmount(annual, Periods.Yearly)
            acc + amountPerWeek
          } else {
            acc
          }
      }
      Logger.debug(s"TCCalculator.TCCalculatorElements.getTotalChildcarePerWeek - End")
      roundDownToThreeDigits(amountPerWeek)
    }

    def maxChildcareElementForPeriod(period: Period) : (Boolean, BigDecimal) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.maxChildcareElementForPeriod")

      val totalCostPerWeek = getTotalChildcarePerWeek(period)
      val totalCostPerWeekRounded = roundToPound(totalCostPerWeek)
      // check threshold amounts
      val amountForPeriod = amountForDateRange(totalCostPerWeekRounded, Periods.Weekly, period.from, period.until, rounded = false, truncated = false)

      val percent = period.config.wtc.eligibleCostCoveredPercent
      val percentOfActualAmountTapered = roundDownToThreeDigits(getPercentOfAmount(amountForPeriod, percent))

      val thresholdAmount = getChildcareThresholdPerWeek(period)
      val thresholdIntoAPeriod = amountForDateRange(thresholdAmount, Periods.Weekly, period.from, period.until, rounded = false, truncated = false)
      val percentOfThresholdAmountTapered = roundDownToThreeDigits(getPercentOfAmount(thresholdIntoAPeriod, percent))

      if(percentOfActualAmountTapered >= percentOfThresholdAmountTapered) {
        (true, round(percentOfThresholdAmountTapered))
      } else {
        (true, round(percentOfActualAmountTapered))
      }
    }

    def generateMaximumAmountsForPeriod(period: Period) = {
      Logger.debug(s"TCCalculator.TCCalculatorElements.generateMaximumAmountsForPeriod")
      val childElement = maxChildElementForPeriod(period)
      val familyElement = maxFamilyElementForPeriod(period)
      val childcareElement = maxChildcareElementForPeriod(period)
      val workingTaxElement = maxWorkElementForPeriod(period) // this contains basic, 30 hour, claimant disability/severe disability + lone/second parent
      models.output.tc.Period(
        from = period.from,
        until = period.until,
        elements = Elements(
          wtcWorkElement = Element(
            maximumAmount = workingTaxElement._2
          ),
          wtcChildcareElement = Element(
            maximumAmount = childcareElement._2
          ),
          ctcIndividualElement = Element(
            maximumAmount = childElement._2
          ),
          ctcFamilyElement = Element(
            maximumAmount = familyElement._2
          )
        )
      )
    }
  }

  trait TCCalculatorHelpers {
    this: TCCalculatorService =>

    /**
     * Unformatted:   5.46102
     * Formatted:     5.47
     *
     * For TC uses a different rounding rules, where rounding always increments the digit prior to a non-zero value
     */
    override def round(value: BigDecimal) = value.setScale(2, RoundingMode.UP)

    /**
     * Unformatted:   5.001
     * Formatted:     6.00
     *
     * For TC uses a different rounding rules, where rounding always increments the digit prior to a non-zero value
     */
    override def roundToPound(value : BigDecimal) = value.setScale(0, RoundingMode.UP)

    def amountFromPeriodToDaily(cost: BigDecimal, fromPeriod: Periods.Period, daysInTheYear : Int): BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.amountFromPeriodToDaily - Begin")
      val amount : BigDecimal = fromPeriod match {
        case Periods.Weekly => (cost * 52) / daysInTheYear
        case Periods.Fortnightly => (cost * 26) / daysInTheYear
        case Periods.Monthly => (cost * 12) / daysInTheYear
        case Periods.Quarterly => (cost * 4) / daysInTheYear
        case Periods.Yearly => cost / daysInTheYear
        case _ => 0.00 //error
      }
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.amountFromPeriodToDaily - End")
      amount
    }

    def amountForDateRange(cost: BigDecimal, period: Periods.Period, fromDate: LocalDate, toDate: LocalDate, rounded : Boolean = true, truncated : Boolean = true) = {
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.amountForDateRange")
      if (fromDate.isBefore(toDate)) {
        //determines if the tax year falls in a leap year and uses 366 days instead of 365 in calculation
        val taxYearDates = TCConfig.getCurrentTaxYearDateRange(fromDate)
        val numberOfDaysInTaxYear = daysBetween(taxYearDates._1, taxYearDates._2)
        //daily amount currently is not rounded

        val dailyAmount = amountFromPeriodToDaily(cost, period, numberOfDaysInTaxYear)
        val dailyAmountTruncated = roundDownToThreeDigits(dailyAmount)

        val numberOfDays = daysBetween(fromDate, toDate)

        rounded match {
          case true =>
            truncated match {
              case true =>
                round(round(dailyAmountTruncated) * numberOfDays)
              case false =>
                round(dailyAmount) * numberOfDays
            }
          case false =>
            truncated match {
              case true =>
                dailyAmountTruncated * numberOfDays
              case false =>
                dailyAmount * numberOfDays
            }
        }
      } else {
        BigDecimal(0.00)
      }
    }

    def getTotalMaximumAmountPerPeriod(period :models.output.tc.Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.getTotalMaximumAmountPerPeriod")
      period.elements.wtcWorkElement.maximumAmount + period.elements.wtcChildcareElement.maximumAmount + period.elements.ctcIndividualElement.maximumAmount + period.elements.ctcFamilyElement.maximumAmount
    }

    def determineTaxYearToProRata(taxYears : List[models.output.tc.TaxYear], proRataEndDate : LocalDate) : (Boolean, Option[models.output.tc.TaxYear]) = {
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.determineTaxYearToProRata - Begin")
      val result = for (ty <- taxYears) yield {
        if (proRataEndDate.toDate.after(ty.from.toDate) && proRataEndDate.toDate.before(ty.until.toDate)) {
          (true, Some(ty))
        } else {
          (false, None)
        }
      }

      val filtered = if (result.exists(c => c._2.isDefined)) result.filter(x => x._2.isDefined) else result
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.determineTaxYearToProRata - End")
      filtered.head
    }

    def proRataTaxYear(taxYear : models.output.tc.TaxYear, proRataStartDate : LocalDate, proRataEndDate : LocalDate) : models.output.tc.TaxYear = {
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.proRataTaxYear")

      val numberOfDaysInTaxYear = daysBetween(taxYear.from, taxYear.until)
      val numberofDaysProRata = daysBetween(proRataStartDate, proRataEndDate)
      //daily amount currently is not rounded
      val dailyAwardAmount = taxYear.taxYearAwardAmount/numberOfDaysInTaxYear
      val proRataAwardAmount = round(roundDownToThreeDigits(dailyAwardAmount  * numberofDaysProRata))

      val dailyAdviceAmount = taxYear.taxYearAdviceAmount/numberOfDaysInTaxYear
      val proRataAdviceAmount =  round(roundDownToThreeDigits(dailyAdviceAmount * numberofDaysProRata))

      models.output.tc.TaxYear(
        from = taxYear.from,
        until = taxYear.until,
        proRataEnd = Some(proRataEndDate),
        taxYearAwardAmount = taxYear.taxYearAwardAmount,
        taxYearAwardProRataAmount = proRataAwardAmount,
        taxYearAdviceAmount = taxYear.taxYearAdviceAmount,
        taxYearAdviceProRataAmount = proRataAdviceAmount,
        periods = taxYear.periods
      )
    }

    def adjustAwardWithProRata(award : TCCalculation, proRataTaxYear: models.output.tc.TaxYear) : models.output.tc.TCCalculation = {
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.adjustAwardWithProRata - Begin")
      val taxYears = for(ty <- award.taxYears) yield {
        if (proRataTaxYear.from == ty.from) {
          proRataTaxYear
        } else {
          ty
        }
      }

      val proRataTotalAward = taxYears.foldLeft(BigDecimal(0.00))((acc, ty) => if (ty.from == proRataTaxYear.from) acc + ty.taxYearAwardProRataAmount else acc + ty.taxYearAwardAmount)
      val proRataTotalAdvice = taxYears.foldLeft(BigDecimal(0.00))((acc, ty) => if (ty.from == proRataTaxYear.from) acc + ty.taxYearAdviceProRataAmount else acc + ty.taxYearAdviceAmount)

      // copy the original calculation swapping out the tax year that has been pro-ratard and adjusting the total calculation value
      val result = award.copy(proRataEnd = proRataTaxYear.proRataEnd, totalAwardProRataAmount = proRataTotalAward, totalHouseHoldAdviceProRataAmount = proRataTotalAdvice, taxYears = taxYears)
      Logger.debug(s"TCCalculator.TCCalculatorHelpers.adjustAwardWithProRata - End")
      result
    }
  }



  class TCCalculatorService extends CCCalculatorService with TCCalculatorElements with TCCalculatorTapering with TCCalculatorHelpers {
    import scala.concurrent.ExecutionContext.Implicits.global

    //reverse taper rate is truncated to 3 decimal places
    //currently using just WTC threshold to calculate advice
    def getAdviceCalculationRounded(totalAmount : BigDecimal, wtcIncomeThreshold : BigDecimal, period: models.input.tc.Period) : BigDecimal = {
      Logger.debug(s"TCCalculator.TCCalculatorService.getAdviceCalculationRounded - Begin")

      val taperPercentage = period.config.thresholds.taperRatePercent
      val reverseTaperRate = BigDecimal(100.00) / BigDecimal(taperPercentage)
      val reverseTaperRateRounded = roundDownToThreeDigits(reverseTaperRate)
      val adviceAmount= round((reverseTaperRateRounded * totalAmount) + wtcIncomeThreshold)
      Logger.debug(s"TCCalculator.TCCalculatorService.getAdviceCalculationRounded - End")
      adviceAmount
    }

    def generateRequiredAmountsPerPeriod(period : models.output.tc.Period, inputPeriod: models.input.tc.Period, income : BigDecimal, wtcIncomeThreshold: BigDecimal, ctcIncomeThreshold: BigDecimal, fullCalculationRequired : Boolean = true) : models.output.tc.Period = {
      Logger.debug(s"TCCalculator.TCCalculatorService.generateRequiredAmountsPerPeriod")
      val totalMaximumAmount = getTotalMaximumAmountPerPeriod(period)

      if(fullCalculationRequired){
        if(isTaperingRequiredForElements(income, wtcIncomeThreshold) && !inputPeriod.atLeastOneClaimantIsClaimingSocialSecurityBenefit) {
          //call taper 1, taper 2, taper 3, taper 4
          val taperedFirstElement = taperFirstElement(period, inputPeriod, income, wtcIncomeThreshold)
          val taperedSecondElement = taperSecondElement(taperedFirstElement,inputPeriod,income,wtcIncomeThreshold)
          val taperedThirdElement = taperThirdElement(taperedSecondElement,inputPeriod,income, wtcIncomeThreshold, ctcIncomeThreshold)
          val taperedFourthElement = taperFourthElement(taperedThirdElement._1,inputPeriod, income, wtcIncomeThreshold, ctcIncomeThreshold, taperedThirdElement._2)

          models.output.tc.Period(
            from = period.from,
            until = period.until,
            elements = Elements(
              wtcWorkElement = taperedFourthElement.elements.wtcWorkElement,
              wtcChildcareElement = taperedFourthElement.elements.wtcChildcareElement,
              ctcIndividualElement = taperedFourthElement.elements.ctcIndividualElement,
              ctcFamilyElement = taperedFourthElement.elements.ctcFamilyElement
            ),
            periodNetAmount = {
              taperedFourthElement.elements.wtcWorkElement.netAmount + taperedFourthElement.elements.wtcChildcareElement.netAmount + taperedFourthElement.elements.ctcIndividualElement.netAmount + taperedFourthElement.elements.ctcFamilyElement.netAmount
            }
          )
        } else { //When no tapering is required
          getPeriodAmount(period, totalMaximumAmount, fullCalculationRequired)
        }
      }
      else {// if calculating household advice
      val adviceAmount = getAdviceCalculationRounded(totalMaximumAmount, wtcIncomeThreshold, inputPeriod)
        getPeriodAmount(period, adviceAmount, fullCalculationRequired)
      }

    }

    def getCalculatedPeriods(taxYear : models.input.tc.TaxYear, previousHouseholdIncome : BigDecimal, fullCalculationRequired : Boolean = true) : List[models.output.tc.Period] = {
      Logger.debug(s"TCCalculator.TCCalculatorService.getCalculatedPeriods - Begin")
      val periods = taxYear.periods
      val calculatedPeriods = for (period <- periods) yield {
        // get all the elements for the period (pro-rota to the number of days) for each household composition
        val income = incomeForPeriod(previousHouseholdIncome, period)
        val wtcIncomeThreshold = wtcIncomeThresholdForPeriod(period)
        val ctcIncomeThreshold = ctcIncomeThresholdForPeriod(period)
        // return an award period which contains all the elements and their amounts they can claim for that period
        val maximumAmounts = generateMaximumAmountsForPeriod(period)
        //here we get the model updated with net due and taper and advice amounts
        val amountForElements = generateRequiredAmountsPerPeriod(maximumAmounts, period, income, wtcIncomeThreshold, ctcIncomeThreshold, fullCalculationRequired)
        //calculate the net due for period
        amountForElements
      }
      Logger.debug(s"TCCalculator.TCCalculatorService.getCalculatedPeriods - End")
      calculatedPeriods
    }

    def getCalculatedTaxYears(inputTCEligibility: TCEligibility, incomeAdviceCalculation: Boolean = false): List[TaxYear] = {
      Logger.debug(s"TCCalculator.TCCalculatorService.getCalculatedTaxYears - Begin")
      val taxYears = inputTCEligibility.taxYears

      val calculatedTaxYears = for (taxYear <- taxYears) yield {
        if(incomeAdviceCalculation) {
          //calculating the income advice
          val calculatedPeriods = getCalculatedPeriods(taxYear, taxYear.houseHoldIncome, fullCalculationRequired = false)
          val adviceAmount = calculatedPeriods.foldLeft(BigDecimal(0.00))((acc, period) => acc + period.periodAdviceAmount)

          models.output.tc.TaxYear(
            from = taxYear.from,
            until = taxYear.until,
            taxYearAdviceAmount = adviceAmount,
            periods = calculatedPeriods
          )
        } else {
          // full calculation including tapering
          val calculatedPeriods = getCalculatedPeriods(taxYear, taxYear.houseHoldIncome, fullCalculationRequired = true)
          val annualAward = calculatedPeriods.foldLeft(BigDecimal(0.00))((acc, period) => acc + period.periodNetAmount)

          models.output.tc.TaxYear(
            from = taxYear.from,
            until = taxYear.until,
            taxYearAwardAmount = annualAward,
            periods = calculatedPeriods
          )
        }
      }
      Logger.debug(s"TCCalculator.TCCalculatorService.getCalculatedTaxYears - End")
      calculatedTaxYears
    }

    override def award(request : Request) : Future[models.output.OutputAPIModel.AwardPeriod] = {

      def createTCCalculation(calculatedTaxYears : List[TaxYear], annualAward: BigDecimal) = {
        Logger.debug(s"TCCalculator.TCCalculatorService.award.createTCCalculation")
        TCCalculation(
          from = calculatedTaxYears.head.from,
          until = {
            if (calculatedTaxYears.length > 1) {
              calculatedTaxYears.tail.head.until
            } else {
              calculatedTaxYears.head.until
            }
          },
          totalAwardAmount = annualAward,
          taxYears = calculatedTaxYears
        )
      }

      def annualAward(taxYears : List[TaxYear]) : BigDecimal = {
        Logger.debug(s"TCCalculator.TCCalculatorService.award.annualAward")
        taxYears.foldLeft(BigDecimal(0.00))((acc, taxYear) => acc + taxYear.taxYearAwardAmount)
      }

      Future {
        request.getTaxCreditsEligibility match {
          case Success(result) =>
            val calculatedTaxYears = getCalculatedTaxYears(result)
            result.proRataEnd match {
              case Some(d) =>
                val taxYearToProRata : (Boolean, Option[TaxYear]) = determineTaxYearToProRata(calculatedTaxYears, d)
                if (taxYearToProRata._1) {
                  val proRateredTaxYear = proRataTaxYear(taxYearToProRata._2.get, taxYearToProRata._2.get.from, d)

                  AwardPeriod(
                    tc = Some(adjustAwardWithProRata(createTCCalculation(calculatedTaxYears, annualAward(calculatedTaxYears)), proRateredTaxYear))
                  )
                } else {
                  AwardPeriod()
                }
              case _ =>
                AwardPeriod(
                  tc = Some(createTCCalculation(calculatedTaxYears, annualAward(calculatedTaxYears)))
                )
            }
          case Failure(e) =>
            AwardPeriod()
        }
      }
    }

    def incomeAdvice(request : Request) : Future[models.output.OutputAPIModel.AwardPeriod] = {

      def createTCCalculation(calculatedTaxYears : List[TaxYear], annualAdvice: BigDecimal) = {
        Logger.debug(s"TCCalculator.TCCalculatorService.incomeAdvice.createTCCalculation")
        TCCalculation(
          from = calculatedTaxYears.head.from,
          until = {
            if (calculatedTaxYears.length > 1) {
              calculatedTaxYears.tail.head.until
            } else {
              calculatedTaxYears.head.until
            }
          },
          houseHoldAdviceAmount = annualAdvice,
          taxYears = calculatedTaxYears
        )
      }

      def annualAdvice(taxYears : List[TaxYear]) : BigDecimal = {
        Logger.debug(s"TCCalculator.TCCalculatorService.incomeAdvice.annualAdvice")
        taxYears.foldLeft(BigDecimal(0.00))((acc, taxYear) => acc + taxYear.taxYearAdviceAmount)
      }

      Future {
        request.getTaxCreditsEligibility match {
          case Success(result) =>
            val calculatedTaxYears = getCalculatedTaxYears(result, incomeAdviceCalculation = true)
            result.proRataEnd match {
              case Some(d) =>
                val taxYearToProRata : (Boolean, Option[TaxYear]) = determineTaxYearToProRata(calculatedTaxYears, d)
                if (taxYearToProRata._1) {
                  val proRateredTaxYear = proRataTaxYear(taxYearToProRata._2.get, taxYearToProRata._2.get.from, d)
                  AwardPeriod(
                    tc = Some(adjustAwardWithProRata(createTCCalculation(calculatedTaxYears, annualAdvice(calculatedTaxYears)), proRateredTaxYear))
                  )
                } else {
                  AwardPeriod()
                }
              case _ =>
                AwardPeriod(
                  tc = Some(createTCCalculation(calculatedTaxYears, annualAdvice(calculatedTaxYears)))
                )
            }
          case Failure(e) =>
            Logger.warn(s"TCCalculator.TCCalculatorService.incomeAdvice.annualAdvice - Exception")
            AwardPeriod()
        }
      }
    }

  }
}
