/*
 * Copyright 2023 HM Revenue & Customs
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

package common.models
import common.mocks.services.MockDateService
import common.models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import common.testConstants.BaseTestConstants.*
import common.testConstants.BusinessDetailsTestConstants.*
import common.testConstants.IncomeSourceDetailsTestConstants.*
import common.testUtils.{TestSupport, UnitSpec}
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

class IncomeSourceDetailsModelSpec extends UnitSpec with Matchers with MockDateService with TestSupport{
  
  "The IncomeSourceDetailsModel" when {

    "the user has both businesses and property income sources" should {

      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        businessesAndPropertyIncome.businesses.head.incomeSourceId shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        businessesAndPropertyIncome.businesses.head.accountingPeriod.get.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        businessesAndPropertyIncome.businesses.head.accountingPeriod.get.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        businessesAndPropertyIncome.businesses.head.tradingName.get shouldBe testTradeName
      }
      //Test Property details
      s"have the property accounting period start date of ${LocalDate.of(2017, 4, 6)}" in {
        businessesAndPropertyIncome.properties.head.accountingPeriod.get.start shouldBe LocalDate.of(2017, 4, 6)
      }
      s"have the property accounting period end date of ${LocalDate.of(2018, 4, 5)}" in {
        businessesAndPropertyIncome.properties.head.accountingPeriod.get.end shouldBe LocalDate.of(2018, 4, 5)
      }
    }

    "the user has just a business income source" should {
      s"have a business ID of $testSelfEmploymentId" in {
        singleBusinessIncome.businesses.head.incomeSourceId shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        singleBusinessIncome.businesses.head.accountingPeriod.get.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        singleBusinessIncome.businesses.head.accountingPeriod.get.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        singleBusinessIncome.businesses.head.tradingName.get shouldBe testTradeName
      }
      "should have latency details" in {
        singleBusinessIncomeWithLatency2019.businesses.head.latencyDetails.get shouldBe testLatencyDetails
      }
      "should have an address" in {
        singleBusinessIncomeWithLatency2019.businesses.head.address.get shouldBe testBizAddress
      }

      //Test Property details
      s"should not have property details" in {
        singleBusinessIncome.properties shouldBe Nil
      }
    }
    "the user has just a property income source" should {
      //Test Property details
      s"have the property accounting period start date of ${LocalDate.of(2017, 4, 6)}" in {
        propertyIncomeOnly.properties.head.accountingPeriod.get.start shouldBe LocalDate.of(2017, 4, 6)
      }
      s"have the property accounting period end date of ${LocalDate.of(2018, 4, 5)}" in {
        propertyIncomeOnly.properties.head.accountingPeriod.get.end shouldBe LocalDate.of(2018, 4, 5)
      }
      //Test Business Details
      "should not have business details" in {
        propertyIncomeOnly.businesses shouldBe List.empty
      }
    }
    "the user has no income source" should {
      "return None for both business and property sources" in {
        noIncomeDetails.properties shouldBe List.empty
        noIncomeDetails.businesses shouldBe List.empty
      }
    }
  }

  "orderedTaxYearsByAccountingPeriods" should {

    "return all tax years from 2018" when {

      "the date service returns a tax year of 2025-26 and the earliest first accounting period end date is in 2017-18" in {
        val model = IncomeSourceDetailsModel(
          nino = testNino,
          mtdbsa = testMtditid,
          yearOfMigration = Some("2020"),
          businesses = List(business1, business2),
          properties = Nil
        )

        setupMockGetCurrentTaxYearEnd(mockDateService)(2026)

        model.orderedTaxYearsByAccountingPeriods(mockDateService) shouldBe List(2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026)
      }
    }
  }

  "orderedTaxYearsByYearOfMigration" should {

    "return all tax years from 2020" when {

      "a year of migration is defined as 2020" in {
        val model = IncomeSourceDetailsModel(
          nino = testNino,
          mtdbsa = testMtditid,
          yearOfMigration = Some("2020"),
          businesses = List(business1, business2),
          properties = Nil
        )

        setupMockGetCurrentTaxYearEnd(mockDateService)(2026)

        model.orderedTaxYearsByYearOfMigration(mockDateService) shouldBe List(2020, 2021, 2022, 2023, 2024, 2025, 2026)
      }
    }

    "return all tax years from 2018" when {

      "a year of migration is not defined and it falls back to the accounting period method" in {
        val model = IncomeSourceDetailsModel(
          nino = testNino,
          mtdbsa = testMtditid,
          yearOfMigration = None,
          businesses = List(business1, business2),
          properties = Nil
        )

        setupMockGetCurrentTaxYearEnd(mockDateService)(2026)

        model.orderedTaxYearsByAccountingPeriods(mockDateService) shouldBe List(2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026)
      }
    }
  }
}