import sbt.Keys._

val sparkVersion = "1.4.1"

val sprayVersion = "1.3.+"

lazy val anomalyDetection = (project in file(".")).enablePlugins(JavaServerAppPackaging).settings (

  name := "anomaly-detection",

  version := "1.0",

  scalaVersion := "2.11.6",

  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.0",
    "io.spray" %% "spray-http" % sprayVersion,
    "io.spray" %% "spray-client" % sprayVersion,
    "io.spray" %% "spray-json" % sprayVersion,
    "mysql"         % "mysql-connector-java" % "5.1.24",
    "joda-time" % "joda-time" % "2.8.1",
    "com.github.nscala-time" %% "nscala-time" % "2.0.0",
    "com.sungevity.analytics" %% "apollo-toolkit" % "1.0.+",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
  ),

  mappings in Universal <<= (mappings in Universal, assembly in Compile) map { (mappings, fatJar) =>
    val filtered = mappings filter { case (file, name) =>  ! name.endsWith(".jar") }
    filtered :+ (fatJar -> ("lib/" + fatJar.getName))
  },

  mainClass := Some("com.sungevity.analytics.Main"),

  assemblyMergeStrategy in assembly := {
    case m if m.startsWith("META-INF/services") => MergeStrategy.concat
    case m if m.startsWith("META-INF") => MergeStrategy.discard
    case c if c.startsWith("reference.") => MergeStrategy.concat
    case _         => MergeStrategy.first
  },

  scriptClasspath := Seq( (assemblyJarName in assembly).value )

)