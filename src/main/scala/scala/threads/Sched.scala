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
      case "takepiece" => result = takepiece(s.totalwork, s.threadnum)
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
      // arr(i) = true
      i += 1
    }
    sum
  }
  
  val queue = new java.util.concurrent.atomic.AtomicReference[List[(Int, Int)]](Nil)
  val totaldone = new java.util.concurrent.atomic.AtomicInteger(0)
  @volatile var totaltodo = 0
  def workdone = totaldone.get == totaltodo
  
  private def addDone(n: Int) = {
    var init = 0
    var upd = 0
    do {
      init = totaldone.get
      upd = init + n
    } while (!totaldone.compareAndSet(init, upd))
  }
  
  @annotation.tailrec private def takefront(t: Int): (Int, Int) = {
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
      var succeeded = queue.compareAndSet(lst, nlst)
      if (succeeded) queue.synchronized {
        queue.notifyAll()
      }
      if (succeeded) hd
      else takefront(t)
    }
  }
  
  val gran = 8
  var workers: Seq[Worker] = null
  
  def takepiece(until: Int, numt: Int) = {
    if (workers == null) {
      workers = for (i <- 1 until numt) yield new Worker
      for (w <- workers) w.start
    }
    
    // init
    totaltodo = until
    totaldone.set(0)
    queue.set(List((0, until)))
    queue.synchronized {
      queue.notifyAll()
    }
    // work until you've got all the pieces done
    var sum = workUntilDone()
    
    sum
  }
  
  private def workUntilDone() = {
    val threshold = totaltodo / gran
    var sum = 0
    while (!workdone) {
      // take a piece and add the rest back
      val hd = takefront(threshold)
      if (hd != null) {
        sum += loop(hd._1, hd._2)
        addDone(hd._2 - hd._1)
      }
      
      queue.synchronized {
        while (queue.get.isEmpty && !workdone) queue.wait()
      }
    }
    sum
  }
  
  class Worker extends Thread {
    this.setDaemon(true)
    override def run() {
      while (true) {
        queue.synchronized {
          if (workdone) queue.notifyAll()
          while (workdone) queue.wait()
        }
        workUntilDone()
      }
    }
  }
  
}










