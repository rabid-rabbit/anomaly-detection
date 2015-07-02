package com.sungevity.analytics

trait SparkApplication[T <: SparkApplicationContext] {

  def run(context: T): Unit

}
