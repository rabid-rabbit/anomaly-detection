package com.sungevity.analytics.model

case class Account(accountID: String, location: Location, installations: Seq[SolarInstallation]) {

  def +(that: Account) = that.accountID match {
    case this.accountID => Account(this.accountID, this.location, that.installations ++ this.installations)
    case _ => this
  }

}

