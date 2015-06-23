package com.sungevity.analytics

import akka.actor.ActorSystem
import breeze.linalg.DenseVector
import com.sungevity.analytics.helpers.Date._
import com.sungevity.analytics.helpers.rest.PriceEngine
import com.sungevity.analytics.helpers.sql.{Queries, ConnectionStrings}
import com.sungevity.analytics.model._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.functions._

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

import utils.Statistics._

object NDayPerformanceAnalyzer extends App {

  implicit val system = ActorSystem()

  def help() {
    println(s"${this.getClass.getSimpleName} <nDays> <nearestNeighbours>")
  }

  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  def toMap(dFrame: DataFrame) = dFrame.map(v => v.schema.fieldNames.zip(v.toSeq).toMap)

  def formatRows(dFrame: DataFrame) = dFrame.take(2).map(v => v.schema.fieldNames.zip(v.toSeq).toMap).mkString("\n")

  if(args.size < 2) {
    println("Please specify number of days and number of nearest neighbours.")
    help()
    sys.exit(1)
  }

  val nDays = 10

  val nearestNeighbours = 50

  println(nDays)

  println(nearestNeighbours)

  def startDate = DateTime.now

  def endDate = startDate + nDays.days

  println(startDate)

  println(endDate)

  def days = dateRange(startDate, endDate, new Period().withDays(1))

  val conf = new SparkConf().setAppName("NDayPerformanceAnalyzer")
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)

  import sqlContext.implicits._

  val allSystemsData = sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.allSystems}) as all_systems",
    "driver" -> "com.mysql.jdbc.Driver"))

  val systemData =  sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.systemData}) as system_data",
    "driver" -> "com.mysql.jdbc.Driver"))

  val productionData =  sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.productionData(DateTime.now - 20.days, DateTime.now - 10.days)}) as production_data",
    "driver" -> "com.mysql.jdbc.Driver"))

  val accounts = allSystemsData.map {
    row =>

      val location = Location(row.getString(1), "", row.getString(2).toDouble, row.getString(3).toDouble)

      val shading = Array(
        row.getDouble(9),
        row.getDouble(10),
        row.getDouble(11),
        row.getDouble(12),
        row.getDouble(13),
        row.getDouble(14),
        row.getDouble(15),
        row.getDouble(16),
        row.getDouble(17),
        row.getDouble(18),
        row.getDouble(19),
        row.getDouble(20)
      )

      val array = SolarArray(row.getString(5),
        row.getDouble(6),
        row.getDouble(7),
        row.getString(8) match {
          case """>6" average standoff""" => 5
          case """>1" to 3" average standoff""" => 3
          case """>0" to 1" average standoff""" => 2
          case """0" average standoff (flush mount or BIPV)""" => 1
          case _ => 4
        },
        shading,
        row.getDouble(21).toString,
        row.getDouble(22).toLong,
        row.getDouble(23).toInt,
        row.getString(24).toBoolean)

      val installation = SolarInstallation(row.getString(4), Seq(array))

      Account(row.getString(0), "", location, Seq(installation))

  }

  val groupedAccounts = accounts.groupBy(account => account.accountID).map( group => group._2.reduce((a, b) => a + b))

  val estimatedMonthlyKwh = groupedAccounts.map {
    account =>

      val result = PriceEngine.monthlyKwh(PERequest("pvwattscontroller", "post", "getProductionEstimation", account))

      (account -> result.response)

  }

  val estimatedDailyKwh = estimatedMonthlyKwh map {
    estimates =>

      val dailyEstimates = estimates._2 flatMap {
        estimate =>

          dateRange(startDate, endDate, new Period().withDays(1)) map {

            day =>

              def avgMonthlyKWh(month: DateTime.Property) = estimate.monthlyOutput(month.get - 1) / month.getMaximumValue

              val dayAvgKwh = ((avgMonthlyKWh((day - 15.days).monthOfYear()) * 15 + avgMonthlyKWh((day + 15.days).monthOfYear()) * 15) / 30) toDouble

              (estimate.id -> dayAvgKwh)

          }

      } groupBy(_._1) flatMap {
        v => v._2.map(_._2)
      }

      (estimates._1 -> dailyEstimates)

  }

  val reports = systemData.join(productionData, productionData("accountNumber") === systemData("accountNumber"), "inner") map {
    row =>

      val dateStringFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

      val byName = (0 until row.schema.length).map( i => row.schema(i).name -> i).toMap

      val location = Location("", row.getString(byName("state")), row.getString(byName("latitude")).toDouble, row.getString(byName("longitude")).toDouble)

      val account = Account(row.getString(byName("accountNumber")), row.getString(byName("name")), location, Seq.empty[SolarInstallation])

      val productionData = ProductionData(row.getString(byName("accountNumber")), new DateTime(row.get(byName("readingDate"))), row.getDouble(byName("reading")))

      Report(account, row.getString(byName("pgVoid")), row.getString(byName("openCase")), row.getString(byName("pgNotes")), List(productionData), new DateTime(row.get(byName("interconnectionDate"))), row.getDouble(byName("actualKwh")))

  }

  val groupedReports = reports.groupBy(r => r.account) map {
    group =>

      println(s"groupedReports ${group._1}")

      val reports = group._2 reduce {
        (a, b) =>
          val readings = a.readings ++ b.readings
          a.copy(
            readings = readings,
            sum = readings.map(_.reading).sum,
            blanksCount = days.length - readings.length,
            smallValuesCount = readings.map(v => if (v.reading <= 1) 1 else 0).sum
          )
      }

      (group._1 -> reports)

  }

  val reportsWithEstimatedDailyKwh = groupedReports.join(estimatedDailyKwh) map {
    entry =>

      println(s"reportsWithEstimatedDailyKwh ${entry._1}")

      val estimatedDailyKwh = entry._2._2
      val report = entry._2._1.copy(
        performanceRatio = entry._2._1.sum / estimatedDailyKwh.sum,
        estimatedReadings = estimatedDailyKwh.toSeq,
        estimatedKwh = estimatedDailyKwh.sum)
      val account = entry._1

      (account -> report)
  }

  val reportsWithEstimatedDailyKwhSample = reportsWithEstimatedDailyKwh sample(false, 0.01) persist

  val cartesian = reportsWithEstimatedDailyKwhSample.cartesian(reportsWithEstimatedDailyKwhSample) filter {
    v =>
      v._1._1 != v._2._1
  }

  val mapped = cartesian map {
    v =>
      (v._1._1 -> Vector((v._1._1.location.haversineDistance(v._2._1.location), v._2._2, v._2._1.location)))
  }

  val grouped = mapped reduceByKey {
    (v1, v2) =>

      (v1 ++ v2).sortBy(_._1).take(50)

  }

  val finalReports = (for {

    group <- reportsWithEstimatedDailyKwhSample.join(grouped)
    account = group._1
    report = group._2._1
    neighborhoodReports = group._2._2.map(_._2)

  } yield {

    println(s"${account.location} -> ${group._2._2.map(v => (v._1, v._3)).toList}")

    val neighborhoodRatios = neighborhoodReports.map(nn => nn.performanceRatio)

    report.copy(
      neighbourhoodPerformanceRatio = neighborhoodRatios.mean,
      neighbourhoodStdDev = neighborhoodRatios.stddev,
      neighbourhoodDevAvg = report.performanceRatio - neighborhoodRatios.mean,
      zScore = (report.performanceRatio - neighborhoodRatios.mean) / neighborhoodRatios.stddev
    )

  }) persist

  import com.sungevity.analytics.helpers.Csv._

  val r = finalReports.repartition(1).toLocalIterator.asCSV.take(5)

  println(r.mkString("\n"))

  system.shutdown()

}
