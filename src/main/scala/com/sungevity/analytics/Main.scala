package com.sungevity.analytics

import java.io.File

import akka.actor.{Props, ActorSystem}
import com.sungevity.analytics.api.SparkApplication
import com.sungevity.analytics.performanceanalyzer.NDayPerformanceAnalyzer
import com.sungevity.analytics.protocol.StartApplication
import com.sungevity.analytics.utils.IOUtils
import com.typesafe.config.ConfigFactory

object Main extends App {

  def help() {
    println(s"\nUsage: ${this.getClass.getName} <command> <configuration file>\n")
  }

  if (args.length < 1) {
    Console.err.println("Incorrect number of input arguments.")
    help()
    sys.exit(1)
  }

  if (!IOUtils.isReadable(args(0))) {
    Console.err.println("Could not open configuration file.")
    sys.exit(2)
  }

  implicit val config = ConfigFactory.parseFile(new File(args(0)))

  implicit val system = ActorSystem("AnomalyDetection", config)

  val actor = system.actorOf(Props(new NDayPerformanceAnalyzer(config)), "nday-performance-analyzer")

}
