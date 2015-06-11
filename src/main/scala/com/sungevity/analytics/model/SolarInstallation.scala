package com.sungevity.analytics.model

case class SolarInstallation(systemID: String, arrays: Seq[SolarArray]) {

  override def toString(): String = {
    s"systemID = [$systemID]"
  }

}
