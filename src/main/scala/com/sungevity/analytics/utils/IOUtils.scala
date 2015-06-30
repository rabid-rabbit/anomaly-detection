package com.sungevity.analytics.utils

import java.nio.file.{Files, Paths, OpenOption}

object IOUtils {

  def write(path: String, txt: String, options: OpenOption*): Unit = {
    import java.nio.file.{Paths, Files}
    import java.nio.charset.StandardCharsets

    Files.write(Paths.get(path), txt.getBytes(StandardCharsets.UTF_8), options:_*)
  }

  def read(path: String): String =
    scala.io.Source.fromFile(path).getLines.mkString

  def isReadable(path: String) = Files.isReadable(Paths.get(path))

  def isWritable(path: String) = Files.isWritable(Paths.get(path))

}
