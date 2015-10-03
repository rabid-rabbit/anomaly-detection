package com.sungevity.analytics.performanceanalyzer

import com.sungevity.analytics.model._
import com.sungevity.analytics.api.{HDFSShare, ScheduledApplication, SparkApplication}
import com.typesafe.config.Config

import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._
import com.sungevity.analytics.utils.Date._
import com.sungevity.analytics.utils.Statistics._
import com.sungevity.analytics.utils.Spark._

import scala.concurrent.ExecutionContext.Implicits.global


class NDayPerformanceAnalyzer(config: Config) extends SparkApplication[NDayPerformanceAnalyzerContext](config) with ScheduledApplication with HDFSShare {

  schedule(applicationContext.config.getString("nday-performance-analyzer.schedule")).recover{
    case t: Throwable => applicationContext.log.error(s"Could not schedule ${applicationContext.applicationName}", t)
  }

  override def initializeApplicationContext(config: Config): NDayPerformanceAnalyzerContext = new NDayPerformanceAnalyzerContext(config)

  override def run(applicationContext: NDayPerformanceAnalyzerContext): Int = {

    publish(DateTimeFormat.forPattern("MM-dd-yyyy").print(DateTime.now) + ".csv", true) {

      val accountsNumber = applicationContext.sc.accumulator(0, "Accounts")

      val reportsNumber = applicationContext.sc.accumulator(0, "Reports")

      val finalReportsNumber = applicationContext.sc.accumulator(0, "Final Reports")

      val allSystemsData = applicationContext.dataSources.allSystemsData

      val estimatedPerformance = applicationContext.dataSources.estimatedPerformance.rdd.map{
        row =>

          val byName = row.byName

          row.getString(byName("accountID")) -> (1 to 12).map(row.getDouble(_)).toSeq

      }

      val systemData = applicationContext.dataSources.systemData(applicationContext.startDate, applicationContext.endDate, applicationContext.nDays)

      val productionData = applicationContext.dataSources.productionData(applicationContext.startDate, applicationContext.endDate)

      val accounts = {

        val accounts = allSystemsData.map {
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

        accounts.groupBy(account => account.accountID).map{
          group =>
            accountsNumber += 1
            group._2.reduce((a, b) => a + b)
        }

      }

      val estimatedMonthlyKwh = accounts.map(account => account.accountID -> account).join(estimatedPerformance).map(_._2)

      val estimatedDailyKwh = estimatedMonthlyKwh map {
        estimates =>

          val monthlyOutput = estimates._2.map(_.toDouble)

          val dailyEstimates = applicationContext.startDate.dateRange(applicationContext.endDate, new Period().withDays(1)) map {

            day =>

              NDayPerformanceAnalyzerUtils.dailyAvg(day, monthlyOutput)

          }

          (estimates._1 -> dailyEstimates.toArray)

      }

      val reports = {

        val reports = systemData.join(productionData, productionData("accountNumber") === systemData("accountNumber"), "inner") flatMap {
          row =>

            val byName = row.byName

            val location = (Option(row.getString(byName("state"))), Option(row.getString(byName("latitude"))), Option(row.getString(byName("longitude")))) match {
              case (Some(name), Some(latitude), Some(longitude)) =>
                Some(Location("", name, latitude.toDouble, longitude.toDouble))
              case _ => None
            }

            location.map {

              location =>

                val account = Account(row.getString(byName("accountNumber")), row.getString(byName("name")), location, Seq.empty[SolarInstallation])

                val productionData = ProductionData(row.getString(byName("accountNumber")), new DateTime(row.get(byName("readingDate"))), row.getDouble(byName("reading")))

                Report(account, row.getString(byName("pgVoid")), row.getString(byName("openCase")), row.getDecimal(byName("count")).doubleValue(), row.getString(byName("pgNotes")), List(productionData), new DateTime(row.get(byName("interconnectionDate"))), row.getDouble(byName("actualKwh")))

            }

        }

        val groupedReports = reports.groupBy(r => r.account) map {
          group =>

            reportsNumber += 1

            val report = group._2 reduce {
              (a, b) =>
                val readings = a.readings ++ b.readings
                a.copy(
                  readings = readings
                )
            }

            (group._1 -> report.copy(
              readimgsSum = report.readings.map(_.reading).sum,
              blanksCount = applicationContext.days.length - report.readings.length,
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

      val persistedReports = applicationContext.sampleFraction match {
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
            (distanceAndReportA ++ distanceAndReportB).sortBy(_._1).take(applicationContext.nearestNeighbours)
        }

      }

      val finalReports = (for {

        group <- persistedReports.join(accountsAndNearestNeighbors)
        account = group._1
        report = group._2._1
        neighborhoodReports = group._2._2.map(_._2)

      } yield {

          finalReportsNumber += 1

          val neighborhoodRatios = neighborhoodReports.map(nn => nn.performanceRatio)

          report.copy(
            neighbourhoodPerformanceRatio = neighborhoodRatios.mean,
            neighbourhoodStdDev = neighborhoodRatios.stddev,
            neighbourhoodDevAvg = report.performanceRatio - neighborhoodRatios.mean,
            zScore = (report.performanceRatio - neighborhoodRatios.mean) / neighborhoodRatios.stddev
          )

        }) persist

      import com.sungevity.analytics.helpers.csv.Csv._

      Seq(accountsNumber, reportsNumber, finalReportsNumber).foreach {
        acc =>
          println(s"${acc.name.getOrElse("Unknown Accumulator")}: [${acc.value}]")
      }

      finalReports.asCSV.map(_ + "\n")

    }

    0

  }

}