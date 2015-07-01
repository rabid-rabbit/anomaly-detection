package com.sungevity.analytics.helpers.rest

import akka.actor.ActorSystem
import com.sungevity.analytics.model.{ProductionEstimation, PEResponse, Account, PERequest}
import com.typesafe.config.Config
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import scala.concurrent.ExecutionContext.Implicits.global

object PriceEngine {

  import com.sungevity.analytics.helpers.rest.protocols.PriceEngine._

  lazy implicit val system = ActorSystem()

  def monthlyKwh(account: PERequest[Account], requestMaxLatency: Duration = 10 seconds): PEResponse[Seq[ProductionEstimation]] = {

    val pipeline: HttpRequest => Future[PEResponse[Seq[ProductionEstimation]]] = sendReceive ~> unmarshal[PEResponse[Seq[ProductionEstimation]]]

    Await result (pipeline(Post("http://brsf.sungevity.com", account)), requestMaxLatency)

  }

}
