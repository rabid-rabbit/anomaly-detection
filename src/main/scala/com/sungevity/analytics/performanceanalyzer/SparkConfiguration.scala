package com.sungevity.analytics.performanceanalyzer

import com.sungevity.analytics.SparkApplicationContext
import org.apache.spark.{SparkContext, SparkConf}

trait SparkConfiguration {

  def context: SparkApplicationContext

  val settings = Seq(
    if(context.config.hasPath("spark.default.parallelism"))
      Option("spark.default.parallelism" -> context.config.getInt("spark.default.parallelism").toString)
    else None,
    Some("spark.cassandra.connection.host" -> context.config.getString("cassandra.connection-host")),
    Some("spark.cleaner.ttl" -> context.config.getString("cassandra.spark-cleaner-ttl"))
  ).flatten

  val conf = new SparkConf().
    setAppName(context.applicationName).
    setAll(settings)

  val sc = new SparkContext(conf)

}
