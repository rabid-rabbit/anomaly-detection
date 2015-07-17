package com.sungevity.analytics.performanceanalyzer

import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._
import com.sungevity.analytics.utils.Date._

object NDayPerformanceAnalyzerUtils {

  def dailyAvg( day: DateTime, monthlyAvg: Seq[Double]) = {

    def avgMonthlyKWh(date: DateTime) = monthlyAvg(date.getMonthOfYear - 1) / date.dayOfMonth().getMaximumValue

    val days = ((day - 15.days) dateRange ((day + 14.days), 1 day)).toList

    (((day - 15.days) dateRange ((day + 14.days), 1 day)) map (avgMonthlyKWh) sum) / 30

  }

}
