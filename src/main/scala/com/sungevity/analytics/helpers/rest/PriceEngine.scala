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

  def monthlyKwh(account: PERequest[Account])(implicit actorSystem: ActorSystem): PEResponse[Seq[ProductionEstimation]] = {

//    println(s"getting estimates for ${account.data.accountID}")

    PEResponse(Seq(ProductionEstimation(account.data.installations.head.systemID, 1034, Seq(42, 55, 80, 105, 126, 127, 133, 116, 94, 72, 48, 37), Seq.empty, None)))

//    val pipeline: HttpRequest => Future[PEResponse[Seq[ProductionEstimation]]] = sendReceive ~> unmarshal[PEResponse[Seq[ProductionEstimation]]]
//
//    Await result (pipeline(Post("http://brsf.sungevity.com", account)), 10 seconds)

  }

}
