package com.sungevity.analytics.model


case class PERequest[T](restController: String, restMethod: String, restAction: String, data: T)

