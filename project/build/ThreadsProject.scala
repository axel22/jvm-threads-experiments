

import sbt._
import Process._



class ThreadsProject(info: ProjectInfo) extends DefaultProject(info) {
  
  val ps = java.io.File.pathSeparator
  val projName = "threads"
  val scalaVersion = "2.8.0"
  val projVersion = "1.0"
  val bootDir = "project" / "boot"
  val bootScalaDir = bootDir / ("scala-" + scalaVersion)
  val bootLibDir = bootScalaDir / "lib"
  val artifactName = projName + "_" + scalaVersion + "-" + projVersion + ".jar"
  val classpath = List(
    bootLibDir / "scala-library.jar",
    "target" / ("scala_" + scalaVersion) / artifactName
  ).mkString(ps)
  
  // actions
  
  def javavm(settings: String, cp: String, mainclass: String) = "java " + settings + " -cp " + cp + " " + mainclass
  
  def runcommand(command: String) = {
    println("Running: " + command)
    command !
  }
  
  lazy val server = task {
    runcommand(javavm("-Xms256m -Xmx512m -server ", classpath, "scala.threads.app"))
    None
  }
  
  lazy val servergc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server ", classpath, "scala.threads.app"))
  }
  
}


