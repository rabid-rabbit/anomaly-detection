package com.sungevity.analytics.model

import org.joda.time.DateTime

case class ProductionData(customerID: String, readingDate: DateTime, reading: Double)
