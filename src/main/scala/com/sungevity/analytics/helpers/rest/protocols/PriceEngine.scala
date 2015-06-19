package com.sungevity.analytics.helpers.rest.protocols

import com.sungevity.analytics.model._
import spray.json._

object PriceEngine extends DefaultJsonProtocol {

  implicit object SolarArrayFormat extends  JsonWriter[SolarArray] {

    def write(array: SolarArray) = {

      val shadings = array.shading map (_.toJson)

      JsObject(
        "array_id" -> array.arrayID.toJson,
        "pitch" -> array.pitch.toJson,
        "azimuth" -> array.azimuth.toJson,
        "standoff_height" -> array.standoffHeight.toJson,
        "shading" -> JsArray(shadings:_*),
        "inverter_id" -> array.inverterID.toJson,
        "module_id" -> array.moduleID.toJson,
        "module_quantity" -> array.moduleQuantity.toJson,
        "is_inverter_shared" -> array.inverterShared.toJson
      )
    }

  }

  implicit object SolarInstallationFormat extends  JsonWriter[SolarInstallation] {

    def write(installation: SolarInstallation) = {

      val arrays = installation.arrays map (_.toJson)

      JsObject(
        "system_id" -> installation.systemID.toJson,
        "arrays" -> JsArray(arrays:_*)
      )
    }

  }

  implicit object AccountFormat extends  JsonWriter[Account] {

    def write(account: Account) = {

      val systems = account.installations map (_.toJson)

      JsObject(
        "country" -> account.location.country.toJson,
        "latitude" -> JsNumber(account.location.latitude),
        "longitude" -> JsNumber(account.location.longitude),
        "systems" -> JsArray(systems:_*)
      )
    }

  }

  implicit object ProductionEstimationReader extends RootJsonReader[ProductionEstimation] {

    def read(value: JsValue) = {

        value.asJsObject.getFields("system_id", "array_id", "annual_kwh", "monthly_output", "arrays", "production_estimation_version") match {

          case Seq(JsString(systemID), JsNumber(annyalKwh), JsArray(monthlyOutput), JsArray(arrays)) =>

            ProductionEstimation(systemID, annyalKwh.toInt, monthlyOutput.map(_.convertTo[Int]), arrays.map(_.convertTo[ProductionEstimation]), None)

          case Seq(JsString(arrayID), JsNumber(annualKwh), JsArray(monthlyOutput), JsString(productionEstimationVersion)) =>

            ProductionEstimation(arrayID, annualKwh.toInt, monthlyOutput.map(_.convertTo[Int]), Seq.empty[ProductionEstimation], Some(productionEstimationVersion))

        }

      }
  }

  implicit object PEResponseReader extends RootJsonReader[PEResponse[Seq[ProductionEstimation]]] {

    def read(value: JsValue) = {

      PEResponse {

        value.asJsObject.getFields("returnData") match {

          case Seq(obj: JsObject) =>

            obj.getFields("systems") match {

              case Seq(JsArray(systems)) => systems.map(_.convertTo[ProductionEstimation])

              case _ => Seq.empty

            }

          case _ => Seq.empty

        }

      }
    }
  }

  implicit object PERequestFormat extends RootJsonWriter[PERequest[Account]] {

    def write(obj: PERequest[Account]): JsValue = {
      JsObject(
        "rest_controller" -> JsString(obj.restController),
        "rest_method" -> JsString(obj.restMethod),
        "rest_action" -> JsString(obj.restAction),
        "params" -> obj.data.toJson
      )
    }

  }

}
