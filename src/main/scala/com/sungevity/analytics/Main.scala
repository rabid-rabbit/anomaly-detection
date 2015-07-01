package com.sungevity.analytics

import java.io.File

import com.sungevity.analytics.utils.IOUtils
import com.typesafe.config.ConfigFactory

object Main extends App {

  def help() {
    println(s"${this.getClass.getSimpleName} <configuration file>")
  }

  if (args.length != 1) {
    println("Incorrect number of input parameters.")
    help()
    sys.exit(1)
  }

  if (!IOUtils.isReadable(args(0))) {
    println("Could not open configuration file.")
    sys.exit(2)
  }

  implicit val config = ConfigFactory.parseFile(new File(args(0)))

  val ctx = new performanceanalyzer.ApplicationContext(config)

  val sources = new performanceanalyzer.ApplicationData(ctx, config)

  performanceanalyzer.Application(sources).run(ctx)

}
