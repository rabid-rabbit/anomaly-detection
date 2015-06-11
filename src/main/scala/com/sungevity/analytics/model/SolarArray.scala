package com.sungevity.analytics.model

case class SolarArray(arrayID: String, pitch: Double, azimuth: Double, standoffHeight: Int, shading: Array[Double], inverterID: String, moduleID: Long, moduleQuantity: Int, inverterShared: Boolean)
