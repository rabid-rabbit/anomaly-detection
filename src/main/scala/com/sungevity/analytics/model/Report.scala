package com.sungevity.analytics.model

import org.joda.time.DateTime
case class Report(account: Account, pgVoid: String, openCase: String, performanceRatio: Double, pgNotes: String, readings: Seq[ProductionData], interconnectionDate: DateTime, actualKwh: Double, estimatedKwh: Double, sum: Double, blanksCount: Int, hasSmallValues: Boolean) {

  override def toString(): String = {
    s"account = [$account], pgVoid = [$pgVoid], openCase = [$openCase], performanceRatio = [$performanceRatio], pgNotes = [$pgNotes], readings = [$readings], interconnectionDate = [$interconnectionDate], actualKwh = [$actualKwh], estimatedKwh = [$estimatedKwh], blanksCount = [$blanksCount], hasSmallValues = [$hasSmallValues]"
  }

}
