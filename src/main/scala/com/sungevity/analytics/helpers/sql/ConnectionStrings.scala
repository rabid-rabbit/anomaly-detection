package com.sungevity.analytics.helpers.sql

import com.typesafe.config.Config

object ConnectionStrings {

  def current(implicit config: Config) = {
    val db = config.getString("db.current")
    config.getString(s"db.$db.connection-string")
  }

}
