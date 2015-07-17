name := "anomaly-detection"

version := "1.0"

scalaVersion := "2.11.6"

val sparkVersion = "1.4.1"

val sprayVersion = "1.3.+"

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
)

mainClass := Some("com.sungevity.analytics.Main")

assemblyMergeStrategy in assembly := {
  case m if m.startsWith("META-INF") => MergeStrategy.discard
  case c if c.startsWith("reference.") => MergeStrategy.concat
  case _         => MergeStrategy.first
}