name := "polskibus"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

val akkaV = "2.4.1"
val akkaStreamV = "2.0-M2"
val scalaTestV = "2.2.5"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.1",
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamV,
  "com.typesafe.akka" %% "akka-http-core-experimental" % akkaStreamV,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaStreamV,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamV,
  "com.typesafe.akka" %% "akka-http-testkit-experimental" % akkaStreamV,
  "org.scalatest" %% "scalatest" % scalaTestV % "test"
)