package scala.threads






import collection._








trait Settings {
  def totalwork: Int
  def threadnum: Int
  def numtests: Int
  def testname: String
  
  def div: Double
  def wdt: Int
  def lastshown: Int
  def logging: Boolean
  def logfile: String
  
  def printSettings {
    println(" Settings ")
    println("----------")
    println("Test name:   " + testname)
    println("Total work:  " + totalwork)
    println("Thread num.: " + threadnum)
    println("Total tests: " + numtests)
  }
}


trait DefaultSettings extends Settings {
  def totalwork = 1000000000
  def threadnum = 1
  def numtests = 25
  
  def testname = {
    //"none"
    //"loop_local_nocomm"
    //"loop_heap_nocomm"
    //"loop_vread"
    "loop_vwrite"
    //"loop_atomic_read"
    //"loop_atomic_write"
    //"loop_atomic_cas"
  }
  
  def div = 1.0e6
  def wdt = 12
  def lastshown = 10
  
  def logging = false
  def logfile = "testlog.txt"
}


object DefaultSettings extends DefaultSettings


class ParsedSettings(allargs: Array[String]) extends DefaultSettings {
  val argmap: Map[String, Any] = {
    val map = mutable.Map[String, Any]()
    for (arg <- allargs map {_ split "="}) arg(0) match {
      case "totalwork" | "threadnum" | "numtests" if arg.length == 2 => map += arg(0) -> arg(1).toInt
      case "testname" | "email" if arg.length == 2 => map += arg(0) -> arg(1)
      case "logging" if arg.length == 2 => map += arg(0) -> arg(1).toBoolean
      case _ => error("unknown parameter '" + arg(0) + "', found in argument list: " + allargs.mkString(","))
    }
    map
  }
  
  override def totalwork = (argmap.get("totalwork") getOrElse super.totalwork).asInstanceOf[Int]
  override def threadnum = (argmap.get("threadnum") getOrElse super.threadnum).asInstanceOf[Int]
  override def logging = (argmap.get("logging") getOrElse super.logging).asInstanceOf[Boolean]
  override def numtests = (argmap.get("numtests") getOrElse super.numtests).asInstanceOf[Int]
  override def testname = (argmap.get("testname") getOrElse super.testname).asInstanceOf[String]
  
  override def toString = "ParsedSettings = (default) + (overridden:  " + argmap.mkString("", ", ", "") + ")"
}











