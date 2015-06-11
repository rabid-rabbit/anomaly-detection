package com.sungevity.analytics.model

case class ProductionEstimation(id: String, annualKwh: Int, monthlyOutput: Seq[Int], components: Seq[ProductionEstimation], version: Option[String])