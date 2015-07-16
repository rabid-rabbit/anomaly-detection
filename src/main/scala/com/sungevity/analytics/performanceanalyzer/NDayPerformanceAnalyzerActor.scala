package com.sungevity.analytics.performanceanalyzer

import com.datastax.spark.connector.cql.CassandraConnector
import com.sungevity.analytics.api.SparkApplication
import com.sungevity.analytics.helpers.Cassandra
import com.typesafe.config.Config

import com.sungevity.analytics.api.SparkApplication._

class NDayPerformanceAnalyzerActor(config: Config) extends SparkApplication[NDayPerformanceAnalyzerContext, Iterator[String]](config) {

  override def initializeApplicationContext(config: Config): NDayPerformanceAnalyzerContext = new NDayPerformanceAnalyzerContext(config)

  override def run(): Iterator[String] = new NDayPerformanceAnalyzer().run(applicationContext)

  CassandraConnector(applicationContext.conf).withSessionDo { session =>
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS ${Cassandra.keyspaceName} WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 }")
    session.execute(s"CREATE TABLE IF NOT EXISTS ${Cassandra.keyspaceName}.est_monthly_kwh (accountID VARCHAR PRIMARY KEY, estimates LIST<INT>)")
  }

}