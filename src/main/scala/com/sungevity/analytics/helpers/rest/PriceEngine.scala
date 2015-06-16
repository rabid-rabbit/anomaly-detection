package com.sungevity.analytics.helpers.rest

import akka.actor.ActorSystem
import com.sungevity.analytics.model.{ProductionEstimation, PEResponse, Account, PERequest}
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport._

import scala.concurrent.{Await, Future}

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

object PriceEngine {

  import com.sungevity.analytics.helpers.rest.protocols.PriceEngine._

  def monthlyKwh(installation: PERequest[Account])(implicit actorSystem: ActorSystem): PEResponse[Seq[ProductionEstimation]] = {

    val pipeline: HttpRequest => Future[PEResponse[Seq[ProductionEstimation]]] = sendReceive ~> unmarshal[PEResponse[Seq[ProductionEstimation]]]

    Await result (pipeline(Post("http://brsf.sungevity.com", installation)), 10 seconds)

  }

}
