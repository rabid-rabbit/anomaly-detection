package com.sungevity.analytics.utils

import breeze.linalg.DenseVector

object Statistics {

  implicit class StatisticalExtensions(seq: Seq[Double]) {

    private lazy val v = DenseVector(seq: _*)

    def median: Double = breeze.stats.median(v)

    def mean: Double = breeze.stats.mean(v)

    def variance: Double = breeze.stats.variance(v)

    def stddev: Double = breeze.stats.stddev(v)

    def minimum: Double = breeze.linalg.min(v)

    def maximum: Double = breeze.linalg.max(v)
  }

  implicit class ArrStatisticalExtensions(arr: Array[Double]) extends StatisticalExtensions(arr.toSeq)

}