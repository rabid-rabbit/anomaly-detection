package com.sungevity.analytics

import com.typesafe.config.Config

abstract class SparkApplicationContext(@transient val config: Config) extends Serializable {

  def applicationName: String

}
