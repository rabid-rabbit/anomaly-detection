package com.sungevity.analytics.helpers

import com.sungevity.analytics.helpers.Date._
import com.sungevity.analytics.model.{Report}

object Csv {

  trait Reportable {

    def asCSVHeader = cells.map(v => s"""${v._1}""").mkString(",")

    def cells: Iterable[(String, Any)]

    def asCSV: String = cells.map(_._2) collect {
      case s: String => s""""$s""""
      case v => Option(v) map (_.toString) getOrElse("")
    } mkString(",")

  }


  implicit class ReportFormat(report: Report) extends Report(
    report.account,
    report.pgVoid,
    report.openCase,
    report.performanceRatio,
    report.pgNotes,
    report.readings,
    report.estimatedReadings,
    report.interconnectionDate,
    report.actualKwh,
    report.estimatedKwh,
    report.sum,
    report.blanksCount,
    report.smallValuesCount,
    report.neighbourhoodPerformanceRatio,
    report.neighbourhoodStdDev,
    report.neighbourhoodDevAvg,
    report.zScore) with Reportable {

    override def cells: Iterable[(String, Any)] = Vector(
      ("Account Number", account.accountID),
      ("Name", account.name),
      ("State", account.location.state),
      ("Latitude", account.location.latitude),
      ("Longitude", account.location.longitude),
      ("Actual kWh", actualKwh),
      ("Est kWh", estimatedKwh),
      ("Performance ratio", performanceRatio),
      ("Count", readings.map(_.reading).sum),
      ("Open Case", openCase),
      ("System Performance Notes", ""),
      ("PG Void", pgVoid),
      ("Interconnection Date", interconnectionDate),
      ("Neighborhood performance ratio", neighbourhoodPerformanceRatio),
      ("Neighborhood standard dev", neighbourhoodStdDev),
      ("Deviation from neighborhood average", neighbourhoodDevAvg),
      ("Z Score", zScore),
      ("Sum", sum),
      ("Blanks", blanksCount),
      ("<=1", smallValuesCount)
    ) ++ readings.map{
      r =>
        (reportDateFormat.print(r.readingDate), r.reading)
    }

  }

  implicit class IterableReportFormat(iterable: Iterator[Report]) extends Iterator[String] {

    val firstLine = Option(iterable.next())

    val header = firstLine.map{
      line =>
        Iterator.single{
          line.asCSVHeader
        } ++ Iterator.single{
          line.asCSV
        }
    } getOrElse(Iterator.empty)

    override def hasNext: Boolean = header.hasNext || iterable.hasNext

    override def next(): String = header.hasNext match {
      case true => header.next
      case false => iterable.next.asCSV

    }

    def asCSV = this

  }


}