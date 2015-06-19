package com.sungevity.analytics.model

case class Location(country: String, state: String, latitude: Double, longitude: Double) {

  /**
   * http://www.cs.nyu.edu/visual/home/proj/tiger/gisfaq.html
   * @param that
   * @return
   */
  def haversineDistance(that: Location): Double = {
    val R = 3956 // is the approximate radius of (a spherical) earth in miles.
    val dlon = that.longitude - this.longitude
    val dlat = that.latitude - this.latitude
    val a = Math.pow(Math.sin(dlat/2), 2) + Math.cos(this.latitude) * Math.cos(that.latitude) * Math.pow(Math.sin(dlon/2), 2)
    val c = 2 * Math.asin(Math.min(1,Math.sqrt(a)))
    R * c
  }

}
