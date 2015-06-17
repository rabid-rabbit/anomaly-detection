package com.sungevity.analytics

import akka.actor.ActorSystem
import com.sungevity.analytics.helpers.rest.PriceEngine
import com.sungevity.analytics.helpers.sql.{Queries, ConnectionStrings}
import com.sungevity.analytics.model._
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.functions._

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._


object NDayPerformanceAnalyzer extends App {

  implicit val system = ActorSystem()

  def help() {
    println(s"${this.getClass.getSimpleName} <nDays> <nearestNeighbours>")
  }

  def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to))

  if(args.size < 2) {
    println("Please specify number of days and number of nearest neighbours.")
    help()
    sys.exit(1)
  }

  val nDays = args(0).toInt

  val nearestNeighbours = args(1).toInt

  val startDate = DateTime.now

  val endDate = startDate + nDays.days

  val days = dateRange(startDate, endDate, new Period().withDays(1))

  val conf = new SparkConf().setAppName("NDayPerformanceAnalyzer")
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)

  import sqlContext.implicits._

  val accountsSystemsData = sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.allSystems}) as all_systems")).persist

  val accounts = accountsSystemsData.map {
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

  val monthlyKwh = groupedAccounts.map {
    account =>

      PriceEngine.monthlyKwh(PERequest("pvwattscontroller", "post", "getProductionEstimation", account))

  }

  val dailyKwh = monthlyKwh.map {
    installations =>

      installations.response flatMap {
        estimate =>

          dateRange(startDate, endDate, new Period().withDays(1)) map {

            day =>

              def avgMonthlyKWh(month: DateTime.Property) = estimate.monthlyOutput(month.get - 1) / month.getMaximumValue

              val dayAvgKwh = (avgMonthlyKWh((day - 15.days).monthOfYear()) * 15 + avgMonthlyKWh((day + 15.days).monthOfYear()) * 15) / 30

              (estimate.id -> dayAvgKwh)

          }

      } groupBy(_._1) map {
        v => (v._1 -> v._2.map(_._2))
      }

  }

  val systemData =  sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.systemData}) as system_data"))


  val productionData =  sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.productionData(DateTime.now - 20.days, DateTime.now - 10.days)}) as production_data"))

  val reports = systemData.join(productionData, productionData("accountNumber") === systemData("accountNumber"), "inner") map {
    row =>

      val dateStringFormat = DateTimeFormat.forPattern("yyyy-MM-dd")

      val byName = (0 until row.schema.length).map( i => row.schema(i).name -> i).toMap

      val location = Location("", row.getString(byName("state")), row.getString(byName("latitude")).toDouble, row.getString(byName("longitude")).toDouble)

      val account = Account(row.getString(byName("accountNumber")), row.getString(byName("name")), location, Seq.empty[SolarInstallation])

      val productionData = ProductionData(row.getString(byName("accountNumber")), new DateTime(row.get(byName("readingDate"))), row.getDouble(byName("reading")))

      Report(account, row.getString(byName("pgVoid")), row.getString(byName("openCase")), 0.0, row.getString(byName("pgNotes")), List(productionData), new DateTime(row.get(byName("interconnectionDate"))), row.getDouble(byName("actualKwh")), 0.0, 0.0, 0, false)

  }

  val groupedReports = reports.groupBy(r => r.account) map {
    group =>

      group._2 reduce {
        (a, b) =>
          val readings = a.readings ++ b.readings
          val readingsMap = readings.map(_.readingDate.getDayOfMonth -> 0).toMap
          a.copy(
            readings = readings,
            sum = readings.foldLeft(0.0)((v, r) => v + r.reading),
            hasSmallValues = readings.foldLeft(false)((v, r) => v || r.reading <= 1),
            blanksCount = days.foldLeft(0)((r, v) => r + readingsMap.get(v.getDayOfMonth).getOrElse(1))
          )
      }

  }

  def toMap(dFrame: DataFrame) = dFrame.map(v => v.schema.fieldNames.zip(v.toSeq).toMap)

  def formatRows(dFrame: DataFrame) = dFrame.take(2).map(v => v.schema.fieldNames.zip(v.toSeq).toMap).mkString("\n")

  println(groupedReports.take(2).mkString("\n"))

  sc.stop()

  system.shutdown()

}
