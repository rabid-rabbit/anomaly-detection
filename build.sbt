name := "anomaly-detection"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
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
  "org.scalanlp" % "breeze_2.10" % "0.11.2",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "com.sungevity.analytics" %% "apollo-toolkit" % "1.0.+",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)

mainClass := Some("com.sungevity.analytics.Main")

assemblyMergeStrategy in assembly := {
  case m if m.startsWith("META-INF") => MergeStrategy.discard
  case c if c.startsWith("reference.") => MergeStrategy.concat
  case _         => MergeStrategy.first
}