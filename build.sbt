organization := "ru.rknrl"

name := "bugs-server"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.17",

  "ch.qos.logback" % "logback-classic" % "1.0.9",

  "com.typesafe.akka" %% "akka-http-core" % "10.0.3",
  "com.typesafe.akka" %% "akka-http" % "10.0.3",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.3",

  "net.liftweb" %% "lift-json" % "3.1.0-M1",

  "org.scalatest" %% "scalatest" % "3.0.1" % "test",

  "com.github.mauricio" %% "mysql-async" % "0.2.21"
)

test in assembly := {}

assemblyJarName in assembly := "bugs.jar"

mainClass in assembly := Some("ru.rknrl.bugs.Server")