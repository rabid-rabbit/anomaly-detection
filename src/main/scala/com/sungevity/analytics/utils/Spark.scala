package com.sungevity.analytics.utils

import org.apache.spark.sql.Row

object Spark {

  implicit class RichRow(row: Row) {

    lazy val byName = (0 until row.schema.length).map(i => row.schema(i).name -> i).toMap

  }

}
