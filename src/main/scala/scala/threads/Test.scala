package scala.threads






trait Test {
  def settings: Settings
  def times: Times
  protected def testBody(args: Array[String])
  
  def runTest(args: Array[String]) = {
    testBody(args)
    
    if (settings.logging) logReport
  }
  
  private def logReport {
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














