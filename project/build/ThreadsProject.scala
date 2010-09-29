

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
  
  // settings
  
  trait Settings {
    def java: String
  }
  
  object defaultSettings extends Settings {
    def java = "java"
  }
  
  var testName = "scala.threads.ThreadTests"
  
  // server settings
  
  trait Server extends Settings {
    val url: String
    def desc: cpu
    def name: String
  }
  
  case class cpu(manuf: String, model: String, cpus: Int, cores: Int, clock: Double) {
    private def cores2str = cores match {
      case 1 => "uniprocessor"
      case 2 => "dual-core"
      case 4 => "quad-core"
      case n => n + "-core"
    }
    override def toString = manuf + " " + model + " " + cpus + "x " + cores2str + " @ " + clock + " GHz"
  }
  
  object mtcquad extends Server {
    def name = "mtcquad"
    def desc = cpu("AMD", "8220", 4, 2, 2.80)
    val url = "mtcquad.epfl.ch"
    def java = "../jdk1.6.0_16/bin/java"
  }
  
  object i7_2x4 extends Server {
    def name = "i7-2x4"
    def desc = cpu("Intel", "i7", 2, 4, 2.67)
    val url = "lamppc18.epfl.ch"
    def java = "../jdk-6u21/java/bin/java"
    // def java = "../jdk1.6.0_16/bin/java"
  }
  
  val servers = Map(
    mtcquad.name -> mtcquad,
    i7_2x4.name -> i7_2x4
  )

  var currentUser = "prokopec"
  
  // definitions
  
  def vm(settings: Settings, jvmsettings: String, cp: String, mainclass: String, args: String*) =
    settings.java + " " + jvmsettings + " -cp " + cp + " " + mainclass + " " + args.foldLeft("")(_ + " " + _)
  
  def javavm(settings: String, cp: String, mainclass: String) = vm(defaultSettings, settings, cp, mainclass)
  
  def ssh(user: String, remoteurl: String, command: String) = 
    "ssh " + user + "@" + remoteurl + " " + command
  
  def scp(user: String, remoteurl: String, src: String, dest: String, mods: String) = 
    "scp " + mods + " " + src + " " + user + "@" + remoteurl + ":" + projName + "/" + dest
  
  def serversbt(user: String, remoteurl: String, sbtcommand: String, sbtargs: String*) =
    ssh(user, remoteurl, "cd " + projName + "; ~/bin/sbt '" + sbtcommand + (sbtargs.foldLeft("")(_ + " " + _)) + "'")
  
  def deploy(user: String, remoteurl: String, filenames: Seq[(String, String, String)], sbtcommand: String, sbtargs: String*) = {
    val init = ssh(user, remoteurl, "mkdir " + projName)
    val copies = filenames flatMap {
      p => List(
        ssh(user, remoteurl, "mkdir " + projName + "/" + p._2),
        scp(user, remoteurl, p._1, p._2, p._3)
      )
    }
    val sbtc = serversbt(user, remoteurl, sbtcommand, (sbtargs: _*))
    List(init) ++ copies ++ List(sbtc)
  }
  
  def deldir(dirname: String) = "rm -rf " + dirname
  
  def clear(user: String, remoteurl: String) = ssh(user, remoteurl, deldir(projName))
  
  def runcommand(command: String) = {
    println("Running: " + command)
    command !
  }
  
  def runcommands(commands: Seq[String]) = {
    for (c <- commands) runcommand(c)
  }
  
  def runtestbatch(settings: Settings, testnm: String) = {
    val info = Tests(testnm)
    println(info)
    for (args <- info) {
      runcommand(vm(settings, "-Xms256m -Xmx512m -server", classpath, testnm, args))
    }
  }
  
  def fs(s: String, len: Int) =
    if (s.length < len) s + (new runtime.RichString(" ") * (len - s.length))
    else s
  
  // tasks
  
  lazy val setUser = task {
    args => if (args.length != 1) task {
      Some("Please specify user. Example: set-user <username>")
    } else task {
      currentUser = args(0)
      None
    }
  }
  
  lazy val setTest = task {
    args => if (args.length != 1) task {
      Some("Please specify which test (full class name). Example: set-test <testname>")
    } else task {
      testName = args(0)
      None
    }
  }
  
  lazy val listServers = task {
    println("Server list:")
    for ((nm, sett) <- servers) println(fs(nm, 12) + " : " + sett.desc + "; url: " + sett.url)
    None
  }
  
  lazy val getUser = task {
    println("Current user: " + currentUser)
    None
  }
  
  lazy val getTest = task {
    println("Test set to: " + testName)
    None
  }
  
  lazy val runClientvm = task {
    runcommand(javavm("-Xms256m -Xmx512m -client", classpath, testName))
    None
  } dependsOn { `package` }
  
  lazy val runServervm = task {
    runcommand(javavm("-Xms256m -Xmx512m -server", classpath, testName))
    None
  } dependsOn { `package` }
  
  lazy val runServervmParGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParallelGC", classpath, testName))
    None
  } dependsOn { `package` }
  
  lazy val runServervmParNewGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParNewGC", classpath, testName))
    None
  } dependsOn { `package` }
  
  lazy val runServervmAt = task {
    args => if (args.length == 2) task {
      val settings = servers(args(0))
      val testnm = args(1)
      runcommand(vm(settings, "-Xms256m -Xmx512m -server", classpath, testnm))
      None
    } dependsOn { `package` } else task {
      Some("Please specify which server and test. Example: run-servervm-at <server-name> <testname>")
    }
  }
  
  lazy val runBatchServervmAt = task {
    args => if (args.length == 2) task {
      val settings = servers(args(0))
      val testnm = args(1)
      runtestbatch(settings, testnm)
      None
    } dependsOn { `package` } else task {
      Some("Please specify which server and test to run exhaustively. Example: run-batch-servervm-at <server-name> <testname>")
    }
  }
  
  private def deploySrcTask(sbtaftercommand: String, sbtargs: String*) = task {
    args => if (args.length == 1) task {
      val server = servers(args(0))
      runcommand(ssh(currentUser, server.url, deldir(projName + "/target")))
      runcommands(
        deploy(currentUser, server.url,
               List(("project/build.properties", "project", ""),
                    (projDefinitionFile, projDefinitionPath, ""),
                    ("src", "/", "-r")),
               sbtaftercommand, (List(server.name) ++ sbtargs): _*)
      )
      None
    } dependsOn { `package` } else task {
      Some("Please specify server. Example: deploy-src <server-name>")
    }
  }
  
  private def deployAtTask(sbtaftercommand: String, sbtargs: String*) = task {
    args => if (args.length == 1) task {
      val sv = servers(args(0))
      runcommand(clear(currentUser, sv.url))
      runcommands(deploy(currentUser, sv.url, List((".",  "", "-r -p")), sbtaftercommand, (List(sv.name) ++ sbtargs): _*))
      None
    } dependsOn { `package` } else task {
      Some("Please specify server to deploy at. Example: deploy-at <server-name>")
    }
  }
  
  lazy val deploySrcRun = deploySrcTask("run-servervm-at", testName)
  
  lazy val clearServer = task {
    args => if (args.length == 1) task {
      val server = servers(args(0))
      runcommand(clear(currentUser, server.url))
      None
    } else task {
      Some("Please specify server to clear. Example: clear-server <server-name>")
    }
  }
  
  lazy val deployRun = deployAtTask("run-servervm-at", testName)
  
  lazy val deployRunBatch = deployAtTask("run-batch-servervm-at", testName)
  
  lazy val deploySrcRunBatch = deployAtTask("run-batch-servervm-at", testName)
  
  lazy val runAt = task {
    args => if (args.length == 1) task {
      val sv = servers(args(0))
      runcommand(serversbt(currentUser, sv.url, "run-servervm-at", sv.name, testName))
      None
    } else task {
      Some("Please specify server to run previously deployed. Example: run-at <server-name>")
    }
  }
}


