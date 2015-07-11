package com.sungevity.analytics.performanceanalyzer

import org.scalatest.WordSpec

import com.sungevity.analytics.utils.Date._
import com.sungevity.analytics.utils.Math._

class NDayPerformanceAnalyzerTest extends WordSpec {

  "NDayPerformanceAnalyzer" should {

    "calculate avg daily performance from monthly data correctly" in {

      assert(NDayPerformanceAnalyzerUtils.dailyAvg("01-01-2015 00:00:00", (1 to 12).map(_.toDouble)) ≈≈ 0.20967741935)
      assert(NDayPerformanceAnalyzerUtils.dailyAvg("06-15-2015 00:00:00", (1 to 12).map(_.toDouble)) ≈≈ 0.19870967741)
      assert(NDayPerformanceAnalyzerUtils.dailyAvg("10-08-2015 00:00:00", (1 to 12).map(_.toDouble)) ≈≈ 0.31655913978)


    }

  }

}
