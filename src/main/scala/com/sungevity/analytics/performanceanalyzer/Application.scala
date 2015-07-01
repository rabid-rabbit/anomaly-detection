package com.sungevity.analytics.performanceanalyzer

import java.nio.file.StandardOpenOption

import com.github.nscala_time.time.Imports._
import com.sungevity.analytics.helpers.Cassandra
import com.sungevity.analytics.helpers.rest.PriceEngine
import com.sungevity.analytics.model._
import com.sungevity.analytics.utils.IOUtils
import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._

import com.sungevity.analytics.utils.Cassandra._
import com.sungevity.analytics.utils.Date._
import com.sungevity.analytics.utils.Statistics._

case class Application(sources: ApplicationData) {

  def run(config: ApplicationContext) {

    val accounts = {

      val accounts = sources.allSystemsData.map {
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

      accounts.groupBy(account => account.accountID).map(group => group._2.reduce((a, b) => a + b))

    }

    val estimatedMonthlyKwh = Cassandra.keyspaceName.getOrElse(accounts, "est_monthly_kwh", (a: Account) => a.accountID) {
      account =>

        println(s"getting estimates for ${account}")

        val result = PriceEngine.monthlyKwh(PERequest("pvwattscontroller", "post", "getProductionEstimation", account), config.requestMaxLatency)

        if (result.response.isEmpty) {
          println(s"could not get estimates for ${account}")
        }

        result.response.headOption.map(_.monthlyOutput.toList)
    }

    val estimatedDailyKwh = estimatedMonthlyKwh map {
      estimates =>

        val monthlyOutput = estimates._2

        val dailyEstimates = config.startDate.dateRange(config.endDate, new Period().withDays(1)) map {

          day =>

            def avgMonthlyKWh(month: DateTime.Property) = monthlyOutput(month.get - 1) / month.getMaximumValue

            val dayAvgKwh = ((avgMonthlyKWh((day - 15.days).monthOfYear()) * 15 + avgMonthlyKWh((day + 15.days).monthOfYear()) * 15) / 30) toDouble

            dayAvgKwh

        }

        (estimates._1 -> dailyEstimates.toArray)

    }

    val reports = {

      val reports = sources.systemData.join(sources.productionData, sources.productionData("accountNumber") === sources.systemData("accountNumber"), "inner") map {
        row =>

          val byName = (0 until row.schema.length).map(i => row.schema(i).name -> i).toMap

          val location = Location("", row.getString(byName("state")), row.getString(byName("latitude")).toDouble, row.getString(byName("longitude")).toDouble)

          val account = Account(row.getString(byName("accountNumber")), row.getString(byName("name")), location, Seq.empty[SolarInstallation])

          val productionData = ProductionData(row.getString(byName("accountNumber")), new DateTime(row.get(byName("readingDate"))), row.getDouble(byName("reading")))

          Report(account, row.getString(byName("pgVoid")), row.getString(byName("openCase")), row.getString(byName("pgNotes")), List(productionData), new DateTime(row.get(byName("interconnectionDate"))), row.getDouble(byName("actualKwh")))

      }

      val groupedReports = reports.groupBy(r => r.account) map {
        group =>

          val report = group._2 reduce {
            (a, b) =>
              val readings = a.readings ++ b.readings
              a.copy(
                readings = readings
              )
          }

          (group._1 -> report.copy(
            readimgsSum = report.readings.map(_.reading).sum,
            blanksCount = config.days.length - report.readings.length,
            smallValuesCount = report.readings.map(v => if (v.reading <= 1) 1 else 0).sum
          ))

      }

      groupedReports.join(estimatedDailyKwh) map {
        entry =>

          val estimatedDailyKwh = entry._2._2
          val report = entry._2._1.copy(
            performanceRatio = entry._2._1.readimgsSum / estimatedDailyKwh.sum,
            estimatedReadings = estimatedDailyKwh.toSeq,
            estimatedKwh = estimatedDailyKwh.sum)
          val account = entry._1

          (account -> report)
      }

    }

    val accountsAndReports = reports sample(false, 0.01) persist

    val accountsAndNearestNeighbors = {

      val cartesian = accountsAndReports.cartesian(accountsAndReports) filter {
        pair =>
          val accountA = pair._1._1
          val accountB = pair._2._1
          accountA != accountB
      }

      val accountsWithDistances = cartesian map {
        pair =>
          val accountA = pair._1._1
          val accountB = pair._2._1
          val reportB = pair._2._2
          accountA -> Vector((accountA.location.haversineDistance(accountB.location), reportB))
      }

      accountsWithDistances reduceByKey {
        (distanceAndReportA, distanceAndReportB) =>
          (distanceAndReportA ++ distanceAndReportB).sortBy(_._1).take(config.nearestNeighbours)
      }

    }

    val finalReports = (for {

      group <- accountsAndReports.join(accountsAndNearestNeighbors)
      account = group._1
      report = group._2._1
      neighborhoodReports = group._2._2.map(_._2)

    } yield {

        val neighborhoodRatios = neighborhoodReports.map(nn => nn.performanceRatio)

        report.copy(
          neighbourhoodPerformanceRatio = neighborhoodRatios.mean,
          neighbourhoodStdDev = neighborhoodRatios.stddev,
          neighbourhoodDevAvg = report.performanceRatio - neighborhoodRatios.mean,
          zScore = (report.performanceRatio - neighborhoodRatios.mean) / neighborhoodRatios.stddev
        )

      }) persist

    import com.sungevity.analytics.helpers.Csv._

    finalReports.repartition(10).toLocalIterator.asCSV.foreach {
      line =>
        IOUtils.write(config.outputPath, s"$line\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    }

  }

}