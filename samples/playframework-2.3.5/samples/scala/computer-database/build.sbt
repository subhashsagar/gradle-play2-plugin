name := "computer-database"

version := "1.0"

libraryDependencies ++= Seq(jdbc, anorm)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")