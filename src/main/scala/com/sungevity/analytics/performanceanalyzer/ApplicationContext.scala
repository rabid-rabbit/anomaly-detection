package com.sungevity.analytics.performanceanalyzer

import com.github.nscala_time.time.Imports._
import com.typesafe.config.Config
import org.joda.time.{DateTime, Period}

import scala.concurrent.duration.Duration

import com.sungevity.analytics.utils.Date._

class ApplicationContext(config: Config) extends Serializable {

  val nDays = config.getInt("input.range")

  val nearestNeighbours = config.getInt("input.nearest-neighbours")

  val outputPath = config.getString("output.path")

  val requestMaxLatency = Duration(config.getString("price-engine.response-max-latency"))

  val startDate = DateTime.now

  val endDate = startDate + nDays.days

  val days = startDate.dateRange(endDate, new Period().withDays(1)).toArray

}
