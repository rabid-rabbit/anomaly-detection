package com.sungevity.analytics.utils

object Math {

  implicit class RichDouble(v: Double){

    def ≈≈(that: Double) = (that - v) < 0.0001

  }

}
