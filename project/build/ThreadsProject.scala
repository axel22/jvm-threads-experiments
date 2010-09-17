

import sbt._
import Process._



class ThreadsProject(info: ProjectInfo) extends DefaultProject(info) {
  
  val ps = java.io.File.pathSeparator
  val projName = "threads"
  val projDefinitionPath = "project/build"
  val projDefinitionFile = projDefinitionPath + "/ThreadsProject.scala"
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
  
  // server settings
  
  val mtcquad = "mtcquad.epfl.ch"
  var currentUser = "prokopec"
  
  // definitions
  
  def mtcquadjava = "../jdk1.6.0_16/bin/java"
  
  def vm(jvm: String, settings: String, cp: String, mainclass: String) = jvm + " " + settings + " -cp " + cp + " " + mainclass
  
  def javavm(settings: String, cp: String, mainclass: String) = vm("java", settings, cp, mainclass)
  
  def ssh(user: String, remoteurl: String, command: String) = 
    "ssh " + user + "@" + remoteurl + " " + command
  
  def scp(user: String, remoteurl: String, src: String, dest: String, mods: String) = 
    "scp " + mods + " " + src + " " + user + "@" + remoteurl + ":" + projName + "/" + dest
  
  def serversbt(user: String, remoteurl: String, sbtcommand: String) = ssh(user, remoteurl, "cd " + projName + "; ~/bin/sbt " + sbtcommand)
  
  def deploy(user: String, remoteurl: String, filenames: Seq[(String, String, String)], sbtcommand: String) = {
    val init = ssh(user, remoteurl, "mkdir " + projName)
    val copies = filenames flatMap {
      p => List(
        ssh(user, remoteurl, "mkdir " + projName + "/" + p._2),
        scp(user, remoteurl, p._1, p._2, p._3)
      )
    }
    val sbtc = serversbt(user, remoteurl, sbtcommand)
    List(init) ++ copies ++ List(sbtc)
  }
  
  def deldir(dirname: String) = "rm -r -f " + dirname
  
  def clear(user: String, remoteurl: String) = ssh(user, remoteurl, deldir(projName))
  
  def runcommand(command: String) = {
    println("Running: " + command)
    command !
  }
  
  def runcommands(commands: Seq[String]) = {
    for (c <- commands) runcommand(c)
  }
  
  // tasks
  
  lazy val setUser = task {
    args => if (args.length != 1) task {
      Some("Please specify user. Example: set-user <username>")
    } else task {
      currentUser = args(0)
      None
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
    runcommand(ssh(currentUser, mtcquad, deldir(projName + "/target")))
    runcommands(
      deploy(currentUser, mtcquad,
             List(("project/build.properties", "project", ""),
                  (projDefinitionFile, projDefinitionPath, ""),
                  ("src", "src", "-r")), 
             "run-servervm-mtc-quad")
    )
    None
  }
  
  lazy val clearMtcQuad = task {
    runcommand(clear(currentUser, mtcquad))
    None
  }
  
  lazy val deployMtcQuad = task {
    runcommand(clear(currentUser, mtcquad))
    runcommands(deploy(currentUser, mtcquad, List((".",  "", "-r -p")), "run-servervm-mtc-quad"))
    None
  } dependsOn { `package` }
  
  lazy val runMtcQuad = task {
    runcommand(serversbt(currentUser, mtcquad, "run-servervm-mtc-quad"))
    None
  }
}


