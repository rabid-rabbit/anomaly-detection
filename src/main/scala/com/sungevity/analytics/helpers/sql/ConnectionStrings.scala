package com.sungevity.analytics.helpers.sql

import com.typesafe.config.Config

object ConnectionStrings {

  def current(implicit config: Config) = {
    val db = config.getString("rdbms.current")
    config.getString(s"rdbms.$db.connection-string")
  }

}
