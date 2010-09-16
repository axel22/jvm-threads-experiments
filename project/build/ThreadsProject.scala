

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
  
  var password = ""
  
  def mtcquadjava = "../jdk1.6.0_16/bin/java"
  
  def vm(jvm: String, settings: String, cp: String, mainclass: String) = jvm + " " + settings + " -cp " + cp + " " + mainclass
  
  def javavm(settings: String, cp: String, mainclass: String) = vm("java", settings, cp, mainclass)
  
  def ssh(user: String, remoteurl: String, command: String) = 
    "ssh " + user + "@" + remoteurl + " " + command
  
  def scp(user: String, remoteurl: String, src: String, dest: String) = 
    "scp -r " + src + " " + user + "@" + remoteurl + ":" + projName + "/" + dest
  
  def serversbt(user: String, remoteurl: String, sbtcommand: String) = ssh(user, remoteurl, "cd " + projName + "; ~/bin/sbt " + sbtcommand)
  
  def deploy(user: String, remoteurl: String, dirnames: Seq[(String, String)], sbtcommand: String) = {
    val init = ssh(user, remoteurl, "rm -r -f " + projName + "; mkdir " + projName)
    val copies = dirnames.map(p => scp(user, remoteurl, p._1, p._2))
    val sbtc = serversbt(user, remoteurl, sbtcommand)
    List(init) ++ copies ++ List(sbtc)
  }
  
  def runcommand(command: String) = {
    println("Running: " + command)
    command !
  }
  
  def runcommands(commands: Seq[String]) = {
    for (c <- commands) runcommand(c)
  }
  
  // tasks
  lazy val setPassword = task {
    args => if (args.length != 1) {
      task { Some("Usage: set-password <server-password>") }
    } else {
      task {
        password = args(0)
        None
      }
    }
  }
  
  lazy val runClientvm = task {
    runcommand(javavm("-Xms256m -Xmx512m -client", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val runServervm = task {
    runcommand(javavm("-Xms256m -Xmx512m -server", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val runServervmMtcQuad = task {
    runcommand(vm(mtcquadjava, "-Xms256m -Xmx512m -server", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val runServervmParGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParallelGC", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val runServervmParNewGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParNewGC", classpath, "scala.threads.app"))
    None
  } dependsOn { `package` }
  
  lazy val deploySrcMtcQuad = task {
    runcommands(deploy("prokopec", "mtcquad.epfl.ch", List(("project", "project"), ("src", "src")), "run-servervm-mtc-quad"))
    None
  } dependsOn { `package` }
  
  lazy val deployMtcQuad = task {
    runcommands(deploy("prokopec", "mtcquad.epfl.ch", List((".",  "")), "run-servervm-mtc-quad"))
    None
  } dependsOn { `package` }
  
  lazy val runMtcQuad = task {
    runcommand(serversbt("prokopec", "mtcquad.epfl.ch", "run-servervm-mtc-quad"))
    None
  }
}


