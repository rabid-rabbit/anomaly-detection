package com.sungevity.analytics.model

import com.sungevity.analytics.helpers.csv.Csv.Reportable
import org.joda.time.DateTime

import com.sungevity.analytics.helpers.Date._

case class Report(account: Account,
                  pgVoid: String,
                  openCase: String,
                  performanceRatio: Double,
                  count: Double,
                  pgNotes: String,
                  readings: Seq[ProductionData],
                  estimatedReadings: Seq[Double],
                  interconnectionDate: DateTime,
                  actualKwh: Double,
                  estimatedKwh: Double,
                  readimgsSum: Double,
                  blanksCount: Int,
                  smallValuesCount: Int,
                  neighbourhoodPerformanceRatio: Double,
                  neighbourhoodStdDev: Double,
                  neighbourhoodDevAvg: Double,
                  zScore: Double) extends Reportable {

  override def cells: Iterable[(String, Any)] = Vector(
    ("Account Number", account.accountID),
    ("Name", account.name),
    ("State", account.location.state),
    ("Latitude", account.location.latitude),
    ("Longitude", account.location.longitude),
    ("Actual kWh", actualKwh),
    ("Est kWh", estimatedKwh),
    ("Performance ratio", performanceRatio),
    ("Count", count),
    ("Open Case", openCase),
    ("System Performance Notes", pgNotes),
    ("PG Void", pgVoid),
    ("Interconnection Date", interconnectionDate),
    ("Neighborhood performance ratio", neighbourhoodPerformanceRatio),
    ("Neighborhood standard dev", neighbourhoodStdDev),
    ("Deviation from neighborhood average", neighbourhoodDevAvg),
    ("Z Score", zScore),
    ("Sum", readimgsSum),
    ("Blanks", blanksCount),
    ("<=1", smallValuesCount)
  ) ++ readings.map{
    r =>
      (reportDateFormat.print(r.readingDate), r.reading)
  }

  override def toString(): String = {
    s"account = [$account]," +
      s" pgVoid = [$pgVoid]," +
      s" openCase = [$openCase]," +
      s" performanceRatio = [$performanceRatio]," +
      s" count = [$count]," +
      s" pgNotes = [$pgNotes]," +
      s" readings = [$readings]," +
      s" estimatedReadings = [$estimatedReadings]," +
      s" interconnectionDate = [$interconnectionDate]," +
      s" actualKwh = [$actualKwh]," +
      s" estimatedKwh = [$estimatedKwh]," +
      s" blanksCount = [$blanksCount]," +
      s" hasSmallValues = [$smallValuesCount]," +
      s" neighbourhoodPerformanceRatio = [$neighbourhoodPerformanceRatio]," +
      s" neighbourhoodStdDev = [$neighbourhoodStdDev]," +
      s" neighbourhoodDevAvg = [$neighbourhoodDevAvg]," +
      s" zScore = [$zScore]"
  }

}

object Report {

  def apply(account: Account, pgVoid: String, openCase: String, count: Double, pgNotes: String, readings: Seq[ProductionData], interconnectionDate: DateTime, actualKwh: Double) =
    new Report(account, pgVoid, openCase, 0.0, count, pgNotes, readings, Seq.empty[Double], interconnectionDate, actualKwh, 0.0, 0.0, 0, 0, 0.0, 0.0, 0.0, 0.0)

}
