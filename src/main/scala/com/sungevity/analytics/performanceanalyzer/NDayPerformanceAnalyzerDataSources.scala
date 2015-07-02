package com.sungevity.analytics.performanceanalyzer

import com.datastax.spark.connector.cql.CassandraConnector
import com.sungevity.analytics.helpers.Cassandra
import com.sungevity.analytics.helpers.sql.{ConnectionStrings, Queries}
import org.apache.spark.sql.SQLContext

case class NDayPerformanceAnalyzerDataSources(context: NDayPerformanceAnalyzerContext) extends SparkConfiguration {

  val sqlContext = new SQLContext(sc)

  val allSystemsData = sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.current(context.config),
    "dbtable" -> s"(${Queries.allSystems}) as all_systems",
    "driver" -> "com.mysql.jdbc.Driver"))

  val systemData = sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.current(context.config),
    "dbtable" -> s"(${Queries.systemData(context.startDate, context.endDate, context.nDays)}) as system_data",
    "driver" -> "com.mysql.jdbc.Driver"))

  val productionData = sqlContext.load("jdbc", Map(
    "url" -> ConnectionStrings.current(context.config),
    "dbtable" -> s"(${Queries.productionData(context.startDate, context.endDate)}) as production_data",
    "driver" -> "com.mysql.jdbc.Driver"))

  CassandraConnector(conf).withSessionDo { session =>
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${Cassandra.keyspaceName} WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    session.execute(s"CREATE TABLE IF NOT EXISTS ${Cassandra.keyspaceName}.est_monthly_kwh (accountID VARCHAR PRIMARY KEY, estimates LIST<INT>)")
  }

}
