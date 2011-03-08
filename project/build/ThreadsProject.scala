

import sbt._
import Process._




class ThreadsProject(info: ProjectInfo) extends DefaultProject(info) {
  
  val ps = java.io.File.pathSeparator
  val projName = "threads"
  val projDefinitionPath = "project/"
  val projDefinitionBuildPath = projDefinitionPath + "build/"
  val projDefinitionFiles = projDefinitionBuildPath
  val scalaVersion = "2.8.1"
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
  
  var testName = "scala.threads.Sched"//ParallelTests"
  var email = "aleksandar.prokopec@gmail.com"
  val tmpfile = "tmp_sbt_2w3e4567cs"
  
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
    def java = "../javas/jdk1.6.0_16/bin/java"
    // def java = "../javas/jdk1.7.0/bin/java"
  }
  
  object i7_2x4 extends Server {
    def name = "i7-2x4"
    def desc = cpu("Intel", "i7", 2, 4, 2.67)
    val url = "lamppc18.epfl.ch"
    def java = "../javas/jdk-6u21/java/bin/java"
    // def java = "../javas/jdk1.6.0_16/bin/java"
  }
  
  val servers = Map(
    mtcquad.name -> mtcquad,
    i7_2x4.name -> i7_2x4
  )

  var currentUser = "prokopec"
  
  // definitions
  
  def vm(settings: Settings, jvmsettings: String, cp: String, mainclass: String, args: String*) =
    settings.java + " " + jvmsettings + " -cp " + cp + " " + mainclass + " " + args.foldLeft("")(_ + " " + _)
  
  def javavm(settings: String, cp: String, mainclass: String, args: String*) = vm(defaultSettings, settings, cp, mainclass, args: _*)
  
  def ssh(user: String, remoteurl: String, command: String) = 
    "ssh " + user + "@" + remoteurl + " " + command
  
  def scp(user: String, remoteurl: String, src: String, dest: String, mods: String) = 
    "scp " + mods + " " + src + " " + user + "@" + remoteurl + ":" + projName + "/" + dest
  
  def serversbt(user: String, remoteurl: String, sbtcommand: String, sbtargs: String*) =
    ssh(user, remoteurl, "cd " + projName + "; ~/bin/sbt '" + sbtcommand + (sbtargs.foldLeft("")(_ + " " + _)) + "'")
  
  def rm(filename: String) = "rm " + filename
  
  def runsendmail(address: String, subject: String, additionaltxt: String, logfile: String) = {
    val command = "(echo 'Subject: " + subject + "'; echo ''; echo '" + additionaltxt + "'; echo ''; cat " + logfile + ") | sendmail " + address
    loginfo("Running: " + command)
    List("sh", "-c", command) !;
  }
  
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
    loginfo("Running: " + command)
    command !
  }
  
  def runcommands(commands: Seq[String]) = {
    for (c <- commands) runcommand(c)
  }
  
  def sendreport(address: String, server: Server, logfile: String, testnm: String) = if (email.trim != "") {
    runsendmail(address, "Test reports for server: " + server.name, "Specs: " + server.desc + "\nTest: " + testnm, logfile)
  }
  
  def loginfo(msg: String) = log.log(Level.Info, msg)
  
  def runtestbatch(server: Server, testnm: String, address: String) = {
    val info = Tests(testnm)
    runcommand(rm(info.logfile))
    for (args <- info) {
      loginfo("Starting test at: " + server.name)
      runcommand(vm(server, "-Xms256m -Xmx512m -server", classpath, testnm, args))
      loginfo("Finished test at: " + server.name)
    }
    sendreport(address, server, info.logfile, testnm)
  }
  
  def fs(s: String, len: Int) =
    if (s.length < len) s + (new runtime.RichString(" ") * (len - s.length))
    else s
  
  // tasks
  
  lazy val setEmail = task {
    args => if (args.length != 1) task {
      Some("Please specify email for sending reports. Example: set-email <email-adress>")
    } else task {
      email = args(0)
      None
    }
  }
  
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
    loginfo("Server list:")
    for ((nm, sett) <- servers) println(fs(nm, 12) + " : " + sett.desc + "; url: " + sett.url)
    None
  }
  
  lazy val getEmail = task {
    loginfo("Email for reports is set to: " + email)
    None
  }
  
  lazy val getUser = task {
    loginfo("Server user is set to: " + currentUser)
    None
  }
  
  lazy val getTest = task {
    loginfo("Test is set to: " + testName)
    None
  }
  
  lazy val runServervm = task {
    args => task {
      runcommand(javavm("-Xms256m -Xmx512m -server", classpath, testName, args: _*))
      None
    } dependsOn { `package` }
  }
  
  lazy val runServervmParGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParallelGC", classpath, testName))
    None
  } dependsOn { `package` }
  
  lazy val runServervmParNewGc = task {
    runcommand(javavm("-Xms256m -Xmx512m -server -XX:+UseParNewGC", classpath, testName))
    None
  } dependsOn { `package` }
  
  lazy val runServervmAt = task {
    args => if (args.length >= 2) task {
      val settings = servers(args(0))
      val testnm = args(1)
      runcommand(vm(settings, "-Xms256m -Xmx512m -server", classpath, testnm, args.drop(2): _*))
      None
    } dependsOn { `package` } else task {
      Some("Please specify which server and test. Example: run-servervm-at <server-name> <testname> [<params>]")
    }
  }
  
  lazy val runBatchServervmAt = task {
    args => if (args.length == 2 || args.length == 3) task {
      val settings = servers(args(0))
      val testnm = args(1)
      val address = if (args.length == 3) args(2) else email
      runtestbatch(settings, testnm, address)
      None
    } dependsOn { `package` } else task {
      Some("Please specify which server, which test to run exhaustively, and the optional e-mail address for the report (omit for default).\n" +
           "Provided only: " + args.mkString(", ") + "\n" +
           "  Example: run-batch-servervm-at <server-name> <testname> [<email-address>]")
    }
  }
  
  private def ifarg(ok: Boolean, str: String) = if (ok) List(str) else Nil
  
  private def deploySrcTask(sbtaftercommand: String, prepserver: Boolean, preptnm: Boolean, sbtargs: String*) = task {
    args => if (args.length >= 1) task {
      val srv = servers(args(0))
      runcommand(ssh(currentUser, srv.url, deldir(projName + "/target")))
      runcommands(
        deploy(
          currentUser,
          srv.url,
          List(
            ("project/build.properties", "project", ""),
            (projDefinitionFiles, projDefinitionPath, "-r"),
            ("src", "/", "-r")
          ),
          sbtaftercommand,
          (ifarg(prepserver, srv.name) ++ ifarg(preptnm, testName) ++ sbtargs): _*
        )
      )
      None
    } dependsOn { `package` } else task {
      Some("Please specify server. Example: deploy-src <server-name>")
    }
  }
  
  private def deployAtTask(sbtaftercommand: String, prepserver: Boolean, preptnm: Boolean, sbtargs: String*) = task {
    args => if (args.length == 1) task {
      val srv = servers(args(0))
      runcommand(clear(currentUser, srv.url))
      runcommands(
        deploy(
          currentUser, 
          srv.url, 
          List((".",  "", "-r -p")), 
          sbtaftercommand, 
          (ifarg(prepserver, srv.name) ++ ifarg(preptnm, testName) ++ sbtargs): _*
        )
      )
      None
    } dependsOn { `package` } else task {
      Some("Please specify server to deploy at. Example: deploy-at <server-name>")
    }
  }
  
  lazy val deploySrcRun = deploySrcTask("run-servervm-at", true, true, "")
  
  lazy val clearServer = task {
    args => if (args.length == 1) task {
      val server = servers(args(0))
      runcommand(clear(currentUser, server.url))
      None
    } else task {
      Some("Please specify server to clear. Example: clear-server <server-name>")
    }
  }
  
  lazy val deployRun = deployAtTask("run-servervm-at", true, true, "")
  
  lazy val deployBatch = deployAtTask("run-batch-servervm-at", true, true, email)
  
  lazy val deploySrcBatch = deploySrcTask("run-batch-servervm-at", true, true, email)
  
  lazy val deploySrcBatchAllServers = task {
    val deployTasks = for ((nm, srv) <- servers) yield {
      deploySrcTask("run-batch-servervm-at", true, true, email)(Array(nm))
    }
    loginfo("Resolving dependencies for the deployment task.")
    for (t <- deployTasks) t.runDependenciesOnly
    val threads = for (t <- deployTasks) yield new Thread {
      override def run {
        t.run
      }
    }
    loginfo("Starting to send batches.")
    threads foreach { _ start }
    threads foreach { _ join }
    loginfo("All batches sent and completed.")
    None
  }
  
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


