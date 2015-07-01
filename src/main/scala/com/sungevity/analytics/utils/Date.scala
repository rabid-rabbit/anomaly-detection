package com.sungevity.analytics.utils

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

object Date {

  implicit class RichDate(date: DateTime) {

    def dateRange(to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(date)(_.plus(step)).takeWhile(!_.isAfter(to))

  }

}


