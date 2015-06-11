name := "anomaly-detection"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.3.1",
  "org.apache.spark" %% "spark-sql" % "1.3.1",
  "io.spray" %% "spray-http" % "1.3.+",
  "io.spray" %% "spray-can" % "1.3.+",
  "io.spray" %% "spray-client" % "1.3.+",
  "io.spray" %% "spray-json" % "1.3.+",
  "mysql"         % "mysql-connector-java" % "5.1.24"
)