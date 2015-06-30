name := "anomaly-detection"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.4.+",
  "org.apache.spark" %% "spark-sql" % "1.4.+",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.4.+",
  "io.spray" %% "spray-http" % "1.3.+",
  "io.spray" %% "spray-can" % "1.3.+",
  "io.spray" %% "spray-client" % "1.3.+",
  "io.spray" %% "spray-json" % "1.3.+",
  "mysql"         % "mysql-connector-java" % "5.1.24",
  "joda-time" % "joda-time" % "2.8.1",
  "com.github.nscala-time" %% "nscala-time" % "2.0.0",
  "org.scalanlp" % "breeze_2.10" % "0.11.2"
)

mainClass := Some("com.sungevity.analytics.NDayPerformanceAnalyzer")

assemblyMergeStrategy in assembly := {
  case m if m.startsWith("META-INF") => MergeStrategy.discard
  case _         => MergeStrategy.first
}