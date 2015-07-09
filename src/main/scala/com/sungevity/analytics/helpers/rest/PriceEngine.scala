package com.sungevity.analytics.helpers.rest

import akka.actor.ActorSystem
import com.sungevity.analytics.model.{ProductionEstimation, PEResponse, Account, PERequest}
import org.slf4j.LoggerFactory
import spray.client.pipelining._
import spray.http.{HttpResponse, HttpRequest}
import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import scala.concurrent.ExecutionContext.Implicits.global

object PriceEngine {

  val log = LoggerFactory.getLogger(getClass.getName)

  import com.sungevity.analytics.helpers.rest.protocols.PriceEngine._

  lazy implicit val system = ActorSystem()

  val logRequest: HttpRequest => HttpRequest = { r => log.debug(r.entity.data.asString); r }

  val logResponse: HttpResponse => HttpResponse = {
    r =>

      r.status.intValue match {
        case 200 => log.debug(s"PE response: [${r.status.intValue}], [${r.entity.data.asString}]")
        case _ => log.warn(s"PE response: [${r.status.intValue}], [${r.entity.data.asString}]")
      }

      r
  }

  def monthlyKwh(account: PERequest[Account], requestMaxLatency: Duration = 10 seconds): PEResponse[Seq[ProductionEstimation]] = {

    try {

      log.info(s"monthlyKwh for [${account.data.accountID}]")

      val pipeline: HttpRequest => Future[PEResponse[Seq[ProductionEstimation]]] = logRequest ~> sendReceive ~> logResponse ~> unmarshal[PEResponse[Seq[ProductionEstimation]]]

      Await result(pipeline(Post("http://brsf.sungevity.com", account)), requestMaxLatency)

    } catch {

      case t: Throwable => {
        log.error("Could not make a request to PE", t)
        PEResponse(Seq.empty)
      }

    }

  }

}
