name := "machinomics"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.2",
  "com.github.nscala-time" %% "nscala-time" % "2.10.0",
  "org.scodec" %% "scodec-core" % "1.9.0",
  "org.mapdb" % "mapdb" % "3.0.0-M5"
)
