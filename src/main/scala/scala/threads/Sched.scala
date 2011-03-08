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
      body(settings)
    }
  }
  
  def body(s: Settings) = {
    if (s.logging) arr = new Array[Boolean](s.totalwork)
    s.testname match {
      case "loop" => result = loop(0, s.totalwork)
      case "takepiece" => result = takepiece(s.totalwork)
    }
    if (s.logging) assert(arr.reduceLeft(_ && _))
  }
  
  val f = (x: Int) => {
    if (x % 2 == 0) x + 3
    else x * 2
  }
  
  @volatile var result = 0
  var arr: Array[Boolean] = null
  
  def loop(start: Int, until: Int) = {
    var i = start
    var sum = 0
    while (i < until) {
      sum += f(i)
      arr(i) = true
      i += 1
    }
    sum
  }
  
  val queue = new java.util.concurrent.atomic.AtomicReference[List[(Int, Int)]](Nil)
  
  private def takefront(t: Int): (Int, Int) = {
    val lst = queue.get
    if (lst.isEmpty) null
    else {
      var hd = lst.head
      var nlst = lst.tail
      while ((hd._2 - hd._1) > t) {
        val len = hd._2 - hd._1
        val div = len / 2
        val nhd = (hd._1, hd._1 + div);
        nlst = (hd._1 + div, hd._2) :: nlst;
        hd = nhd
      }
      queue.compareAndSet(lst, nlst)
      hd
    }
  }
  
  def takepiece(until: Int) = {
    // init
    val threshold = until / 16
    queue.set(List((0, until)))
    
    // work until you've got all the pieces done
    var cond = true
    var sum = 0
    while (cond) {
      // take a piece and add the rest back
      val hd = takefront(threshold)
      sum += loop(hd._1, hd._2)
      
      if (queue.get.isEmpty) cond = false
    }
    
    sum
  }
  
}










