package com.sungevity.analytics.performanceanalyzer

import com.github.nscala_time.time.Imports._
import com.sungevity.analytics.SparkApplicationContext
import com.typesafe.config.Config
import org.joda.time.{DateTime, Period}

import scala.concurrent.duration.Duration

import com.sungevity.analytics.utils.Date._

class NDayPerformanceAnalyzerContext(config: Config) extends SparkApplicationContext(config) {

  val applicationName = "NDayPerformanceAnalyzer"

  val nDays = config.getInt("input.range")

  val nearestNeighbours = config.getInt("input.nearest-neighbours")

  val outputPath = config.getString("output.path")

  val requestMaxLatency = Duration(config.getString("price-engine.response-max-latency"))

  val startDate = DateTime.now - nDays.days

  val endDate = DateTime.now - 1

  val days = startDate.dateRange(endDate, new Period().withDays(1)).toArray

}
