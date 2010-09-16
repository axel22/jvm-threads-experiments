

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
  val scalaLibPath = bootLibDir / "scala-library.jar"
  val artifactPath = "target" / ("scala_" + scalaVersion) / artifactName
  val classpath = List(
    scalaLibPath,
    artifactPath
  ).mkString(ps)
  
  // definitions
  
  def mtcquadjava = "../jdk1.6.0_16/bin/java"
  
  def vm(jvm: String, settings: String, cp: String, mainclass: String) = jvm + " " + settings + " -cp " + cp + " " + mainclass
  
  def javavm(settings: String, cp: String, mainclass: String) = vm("java", settings, cp, mainclass)
  
  def ssh(user: String, remoteurl: String, command: String) = "ssh " + user + "@" + remoteurl + " " + command
  
  def scp(user: String, remoteurl: String, directory: String) = "scp -r " + directory + " " + user + "@" + remoteurl + ":" + projName + "/"
  
  def deploy(user: String, remoteurl: String, sbtcommand: String) = List(
    ssh(user, remoteurl, "rm -r -f " + projName),
    scp(user, remoteurl, "."),
    ssh(user, remoteurl, "cd " + projName + "; ~/bin/sbt " + sbtcommand)
  )
  
  def runcommand(command: String) = {
    println("Running: " + command)
    command !
  }
  
  def runcommands(commands: Seq[String]) = {
    for (c <- commands) runcommand(c)
  }
  
  // tasks
  
  lazy val servervm = task {
    runcommand(javavm("-Xms256m -Xmx512m -server", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val servervmMtcQuad = task {
    runcommand(vm(mtcquadjava, "-Xms256m -Xmx512m -server", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val servervmParGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParallelGC", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val servervmParNewGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParNewGC", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val deployMtcQuad = task {
    runcommands(deploy("prokopec", "mtcquad.epfl.ch", "servervm-mtc-quad"))
    None
  } dependsOn { `package` }
}


