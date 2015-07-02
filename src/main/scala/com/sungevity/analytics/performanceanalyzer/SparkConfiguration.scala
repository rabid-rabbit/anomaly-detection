package com.sungevity.analytics.performanceanalyzer

import com.sungevity.analytics.SparkApplicationContext
import org.apache.spark.{SparkContext, SparkConf}

trait SparkConfiguration {

  def context: SparkApplicationContext

  val conf = new SparkConf().
    setAppName(context.applicationName).
    set("spark.cassandra.connection.host", context.config.getString("cassandra.connection-host")).
    set("spark.cleaner.ttl", context.config.getString("cassandra.spark-cleaner-ttl"))

  val sc = new SparkContext(conf)

}
