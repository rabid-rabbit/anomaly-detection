package com.sungevity.analytics.performanceanalyzer

import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}

trait SparkConfiguration {

  def config: Config

  val conf = new SparkConf().
    setAppName("NDayPerformanceAnalyzer").
    set("spark.cassandra.connection.host", config.getString("cassandra.connection-host")).
    set("spark.cleaner.ttl", config.getString("cassandra.spark-cleaner-ttl"))

  val sc = new SparkContext(conf)

}
