package com.sungevity.analytics.model

import org.joda.time.DateTime
case class Report(account: Account, pgVoid: String, openCase: String, performanceRatio: Double, pgNotes: String, readings: Seq[ProductionData], estimatedReadings: Seq[Double],  interconnectionDate: DateTime, actualKwh: Double, estimatedKwh: Double, sum: Double, blanksCount: Int, hasSmallValues: Boolean) {

  override def toString(): String = {
    s"account = [$account], pgVoid = [$pgVoid], openCase = [$openCase], performanceRatio = [$performanceRatio], pgNotes = [$pgNotes], readings = [$readings], estimatedReadings = [$estimatedReadings], interconnectionDate = [$interconnectionDate], actualKwh = [$actualKwh], estimatedKwh = [$estimatedKwh], blanksCount = [$blanksCount], hasSmallValues = [$hasSmallValues]"
  }

}

object Report {

  def apply(account: Account, pgVoid: String, openCase: String, pgNotes: String, readings: Seq[ProductionData], interconnectionDate: DateTime, actualKwh: Double) =
    new Report(account, pgVoid, openCase, 0.0, pgNotes, readings, Seq.empty[Double], interconnectionDate, actualKwh, 0.0, 0.0, 0, false)
}
