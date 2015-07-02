package com.sungevity.analytics

import java.io.File

import com.sungevity.analytics.performanceanalyzer.{NDayPerformanceAnalyzer, NDayPerformanceAnalyzerContext}
import com.sungevity.analytics.utils.IOUtils
import com.typesafe.config.{Config, ConfigFactory}

import utils.String._

object Main extends App {

  val commandsRegistry = List(
    ((c: Config) => new NDayPerformanceAnalyzerContext(c), new NDayPerformanceAnalyzer)
  )

  def help() {
    println(s"${this.getClass.getSimpleName} <command> <configuration file>")
  }

  if (args.length < 2) {
    println("Incorrect number of input parameters.")
    help()
    sys.exit(1)
  }

  if (!IOUtils.isReadable(args(1))) {
    println("Could not open configuration file.")
    sys.exit(2)
  }

  implicit val config = ConfigFactory.parseFile(new File(args(1)))

  for {
    entry <- commandsRegistry
    context = entry._1(config)
    command = entry._2.asInstanceOf[SparkApplication[SparkApplicationContext]] if context.applicationName.toLowerCase.levenshteinDistance(args(0).toLowerCase) < 2
  } yield {
    command.run(context)
  }

}
