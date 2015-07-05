package com.sungevity.analytics.performanceanalyzer

import java.nio.file.StandardOpenOption

import com.sungevity.analytics.SparkApplication
import com.sungevity.analytics.helpers.Cassandra
import com.sungevity.analytics.helpers.rest.PriceEngine
import com.sungevity.analytics.model._
import com.sungevity.analytics.utils.IOUtils
import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._

import com.sungevity.analytics.utils.Cassandra._
import com.sungevity.analytics.utils.Date._
import com.sungevity.analytics.utils.Statistics._
import com.sungevity.analytics.utils.Spark._
import org.slf4j.LoggerFactory

class NDayPerformanceAnalyzer extends SparkApplication[NDayPerformanceAnalyzerContext] {

  val log = LoggerFactory.getLogger(getClass.getName)

  override def run(context: NDayPerformanceAnalyzerContext) {

    val sources = new NDayPerformanceAnalyzerDataSources(context)

    val accounts = {

      val accounts = sources.allSystemsData.map {
        row =>

          val byName = row.byName

          val location = Location(row.getString(byName("country")), "", row.getString(byName("latitude")).toDouble, row.getString(byName("longitude")).toDouble)

          val shading = (9 to 20).map(row.getDouble(_)).toArray

          val array = SolarArray(
            row.getString(byName("arrayID")),
            row.getDouble(byName("pitch")),
            row.getDouble(byName("azimuth")),
            row.getString(byName("standoffHeight")) match {
              case """>6" average standoff""" => 5
              case """>1" to 3" average standoff""" => 3
              case """>0" to 1" average standoff""" => 2
              case """0" average standoff (flush mount or BIPV)""" => 1
              case _ => 4
            },
            shading,
            row.getDouble(byName("inverterID")).toString,
            row.getDouble(byName("moduleID")).toLong,
            row.getDouble(byName("moduleQuantity")).toInt,
            row.getString(byName("isInverterShared")).toBoolean
          )

          val installation = SolarInstallation(row.getString(byName("systemID")), Seq(array))

          Account(row.getString(byName("accountNumber")), "", location, Seq(installation))

      }

      accounts.groupBy(account => account.accountID).map(group => group._2.reduce((a, b) => a + b))

    }

    val priceEngineEmptyResponses = sources.sc.accumulator(0, "My Accumulator")

    val estimatedMonthlyKwh = Cassandra.keyspaceName.getOrElse(accounts, "est_monthly_kwh", (a: Account) => a.accountID) {
      account =>

        val result = PriceEngine.monthlyKwh(PERequest("pvwattscontroller", "post", "getProductionEstimation", account), context.requestMaxLatency)

        if (result.response.isEmpty) {
          priceEngineEmptyResponses += 1
        }

        result.response.headOption.map(_.monthlyOutput.toList)
    }

    val estimatedDailyKwh = estimatedMonthlyKwh map {
      estimates =>

        val monthlyOutput = estimates._2.map(_.toDouble)

        val dailyEstimates = context.startDate.dateRange(context.endDate, new Period().withDays(1)) map {

          day =>

            def avgMonthlyKWh(month: DateTime.Property) = monthlyOutput(month.get - 1) / month.getMaximumValue

            val dayAvgKwh = ((avgMonthlyKWh((day - 15.days).monthOfYear()) * 15 + avgMonthlyKWh((day + 15.days).monthOfYear()) * 15) / 30)

            dayAvgKwh

        }

        (estimates._1 -> dailyEstimates.toArray)

    }

    val reports = {

      val reports = sources.systemData.join(sources.productionData, sources.productionData("accountNumber") === sources.systemData("accountNumber"), "inner") map {
        row =>

          val byName = row.byName

          val location = Location("", row.getString(byName("state")), row.getString(byName("latitude")).toDouble, row.getString(byName("longitude")).toDouble)

          val account = Account(row.getString(byName("accountNumber")), row.getString(byName("name")), location, Seq.empty[SolarInstallation])

          val productionData = ProductionData(row.getString(byName("accountNumber")), new DateTime(row.get(byName("readingDate"))), row.getDouble(byName("reading")))

          Report(account, row.getString(byName("pgVoid")), row.getString(byName("openCase")), row.getDecimal(byName("count")).doubleValue(), row.getString(byName("pgNotes")), List(productionData), new DateTime(row.get(byName("interconnectionDate"))), row.getDouble(byName("actualKwh")))

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
            blanksCount = context.days.length - report.readings.length,
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

    val persistedReports = context.sampleFraction match {
      case Some(fraction) => reports sample (false, fraction) persist
      case None => reports persist
    }

    val accountsAndNearestNeighbors = {

      val cartesian = persistedReports.cartesian(persistedReports) filter {
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
          (distanceAndReportA ++ distanceAndReportB).sortBy(_._1).take(context.nearestNeighbours)
      }

    }

    val finalReports = (for {

      group <- persistedReports.join(accountsAndNearestNeighbors)
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
        IOUtils.write(context.outputPath, s"$line\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    }

    log.info(s"Empty PE responses: ${priceEngineEmptyResponses.value} accounts")

  }

}