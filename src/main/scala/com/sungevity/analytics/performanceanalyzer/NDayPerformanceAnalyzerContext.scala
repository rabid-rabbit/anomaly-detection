package com.sungevity.analytics.performanceanalyzer

import com.github.nscala_time.time.Imports._
import com.sungevity.analytics.api.SparkApplicationContext
import com.typesafe.config.Config
import org.joda.time.{DateTime, Period}

import scala.concurrent.duration.Duration

import com.sungevity.analytics.utils.Date._

class NDayPerformanceAnalyzerContext(config: Config) extends SparkApplicationContext(config) {

  def applicationName = "NDayPerformanceAnalyzer"

  val nDays = config.getInt("nday-performance-analyzer.input.range")

  val nearestNeighbours = config.getInt("nday-performance-analyzer.input.nearest-neighbours")

  val outputPath = config.getString("nday-performance-analyzer.output.path")

  val requestMaxLatency = Duration(config.getString("price-engine.response-max-latency"))

  val sampleFraction = config.getBoolean("nday-performance-analyzer.sample.on") match {
    case true => Some(config.getDouble("nday-performance-analyzer.sample.fraction"))
    case false => None
  }

  val startDate = DateTime.now - nDays.days

  val endDate = DateTime.now.yesterday

  val days = startDate.dateRange(endDate, new Period().withDays(1)).toArray

}
