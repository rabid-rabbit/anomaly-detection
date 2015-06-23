package com.sungevity.analytics.helpers

import org.joda.time.format.DateTimeFormat

object Date {

  val sqlDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

  val reportDateFormat = DateTimeFormat.forPattern("MM/dd/yyyy");

}
