package scala.threads






trait Test {
  def settings(args: Array[String]): Settings
  protected def testBody(s: Settings): Times
  
  def runTest(args: Array[String]) = {
    val s = settings(args)
    
    val t = testBody(s)
    
    if (s.logging) logReport(s, t)
  }
  
  private def logReport(settings: Settings, times: Times) {
    // open log file
    val out = new java.io.FileOutputStream(settings.logfile, true)
    
    Console.withOut(out) {
      // log settings
      settings.printSettings
      println
      
      // log times
      println(" Times ")
      println("-------")
      times.printAllTimes
      
      // add newline
      println
      println("-" * 80)
      println
    }
  }
}














