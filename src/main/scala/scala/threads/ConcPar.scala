package scala.threads






object ConcPar extends Test {
  def main(args: Array[String]) {
    runTest(args)
  }
  
  def settings(args: Array[String]) = new ParsedSettings(args)
  
  protected def testBody(settings: Settings) = {
    // initialize settings
    val times = new Times(settings)
    import settings._
    import times._
    
    println("Started.")
    
    totalTime = measure {
      for (i <- 0 until numtests) test(times, settings)
    }
    
    println("Done.")
    printAllTimes
    
    times
  }
  
  def test(times: Times, sett: Settings) {
    // TODO
  }
}
