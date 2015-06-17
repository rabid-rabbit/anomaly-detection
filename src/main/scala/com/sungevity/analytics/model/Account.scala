package com.sungevity.analytics.model

case class Account(accountID: String, name: String, location: Location, installations: Seq[SolarInstallation]) {

  def +(that: Account) = that.accountID match {
    case this.accountID => Account(this.accountID, this.name, this.location, that.installations ++ this.installations)
    case _ => this
  }

  override def equals(that: Any): Boolean = {
      that.isInstanceOf[Account] && that.asInstanceOf[Account].accountID == this.accountID
  }

  override def hashCode(): Int = accountID.hashCode

}

