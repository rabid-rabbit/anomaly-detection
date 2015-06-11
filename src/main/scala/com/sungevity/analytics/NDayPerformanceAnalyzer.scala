package com.sungevity.analytics

import com.sungevity.analytics.helpers.rest.PriceEngine
import com.sungevity.analytics.helpers.sql.{Queries, ConnectionStrings}
import com.sungevity.analytics.model._
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}


object NDayPerformanceAnalyzer extends App {

  val conf = new SparkConf().setAppName("NDayPerformanceAnalyzer")
  val sc = new SparkContext(conf)
  val sqlContext = new SQLContext(sc)

  import sqlContext.implicits._

  val accountsSystemsData = sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.local,
    "dbtable" -> s"(${Queries.selectAllSystems}) as all_systems")).persist

  val accounts = accountsSystemsData.map {
    row =>

      val location = Location(row.getString(1), row.getString(2).toDouble, row.getString(3).toDouble)

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

      Account(row.getString(0), location, Seq(installation))

  }

  val groupedAccounts = accounts.groupBy(account => account.accountID).map( group => group._2.reduce((a, b) => a + b))

  val result = groupedAccounts.map {
    account =>

      PriceEngine.monthlyKwh(PERequest("pvwattscontroller", "post", "getProductionEstimation", account))

  }

  println(result.take(1).toList)

}
