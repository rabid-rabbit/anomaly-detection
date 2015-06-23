package com.sungevity.analytics.model


case class Location(country: String, state: String, latitude: Double, longitude: Double) {

  /**
   * http://www.movable-type.co.uk/scripts/latlong.html
   * @param that
   * @return
   */
  def haversineDistance(that: Location): Double = {

    import Math._

    def radians(degrees: Double) = degrees * PI / 180

    val R = 6371000 // metres
    val φ1 = radians(this.latitude)
    val φ2 = radians(that.latitude)
    val Δφ = radians(that.latitude - this.latitude)
    val Δλ = radians(that.longitude - this.longitude)

    val a = sin(Δφ / 2) * sin(Δφ / 2) + cos(φ1) * cos(φ2) * sin(Δλ / 2) * sin(Δλ / 2)

    R * 2 * atan2(sqrt(a), sqrt(1 - a));
  }

  //  3956 * 2 * ASIN
  //  (
  //    SQRT
  //      (
  //          POWER( SIN((geo2.Latitude  - abs(geo.Latitude)) * pi()/180 /2), 2 )
  //            +
  //            POWER( SIN((geo2.Longitude - geo.Longitude)     * pi()/180 /2), 2 )
  //              * COS(   geo2.Latitude  * pi()/180)
  //              * COS(abs(geo.Latitude) * pi()/180)
  //        )
  //    )
  def haversineDistanceOrig(that: Location): Double = {
    import Math._
    val R = 3956 // is the approximate radius of (a spherical) earth in miles.
    val a = pow(sin(that.latitude - abs(this.latitude)) * PI / 180 / 2, 2)
    val b = pow(sin(that.longitude - this.longitude) * PI / 180 / 2, 2) * cos(that.latitude * PI / 180) * cos(abs(this.latitude) * PI / 180)
    val c = R * 2 * asin(sqrt(a + b))
    R * c
  }

}
