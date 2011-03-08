package scala.threads




import collection._






object Sched extends Test {
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
      for (i <- 0 until numtests) {
        test(times, settings)
      }
    }
    
    println("Done.")
    printAllTimes
    
    times
  }
  
  def test(times: Times, settings: Settings) {
    import times._
    import settings._
    
    runTimes += measure {
      body()
    }
  }
  
  def body() {
    
  }
  
}










