package scala.threads




import collection._






object ThreadTests extends ParallelTests {
  def main(args: Array[String]) = runTest(args)
}


trait ParallelTests extends Test {
self =>
  var runargs: Array[String] = _
  lazy val settings: Settings = {
    new ParsedSettings(runargs)
  }
  lazy val times: Times = new Times(settings)
  import times._
  import settings._
  
  protected def testBody(args: Array[String]) {
    // initialize settings
    runargs = args
    settings
    times
    
    println("Started.")
    
    totalTime = measure {
      for (i <- 0 until numtests) test
    }
    
    println("Done.")
    printAllTimes
  }
  
  def test {
    val threads = for (i <- 0 until threadnum) yield new WorkerThread {
      override def run {
        startTimes(i) += timeStamp
        threadTimes(i) += measure {
          val result = call(testname)
        }
      }
    }
    
    runTimes += measure {
      threads foreach { _ start }
      threads foreach { _ join }
    }
  }
  
  class WorkerThread extends Thread {
    var cnt: Int = 0
    @volatile var vcnt: Int = 0
    val atomic_cnt = new java.util.concurrent.atomic.AtomicInteger(0)
    
    def call(name: String) = name match {
      case "none" => // do nothing
      case "loop_heap_write" => loop_heap_write
      case "loop_heap_read" => loop_heap_read
      case "loop_local_write" => loop_local_write
      case "loop_vread" => loop_vread
      case "loop_vwrite" => loop_vwrite
      case "loop_atomic_read" => loop_atomic_read
      case "loop_atomic_write" => loop_atomic_write
      case "loop_atomic_cas" => loop_atomic_cas
      case _ => error("unknown test '" + name + "'")
    }
    
    def loop_local_write = {
      var localcnt: Int = 0
      var i = 0
      var j = 0
      val until = totalwork / threadnum
      while (i < until) {
        if (localcnt >= 0) localcnt += 1
        i += 1
      }
      localcnt
    }
    
    def loop_heap_write = {
      var i = 0
      val until = totalwork / threadnum
      while (i < until) {
        if (cnt >= 0) cnt += 1
        i += 1
      }
      cnt
    }
    
    def loop_heap_read = {
      var i = 0
      val until = totalwork / threadnum
      while (i < until) {
        if (cnt < 0) println("counter negative")
        i += 1
      }
      cnt + i
    }
    
    def loop_vread = {
      var i = 0
      val until = totalwork / threadnum
      while (i < until) {
        if (vcnt < 0) i = until
        i += 1
      }
      vcnt + i
    }
    
    def loop_vwrite = {
      var i = 0
      val until = totalwork / threadnum
      while (i < until) {
        vcnt = i + cnt
        i += 1
      }
      vcnt + i
    }
    
    def loop_atomic_read = {
      var i = 0
      val until = totalwork / threadnum
      val acnt = atomic_cnt
      while (i < until) {
        if (acnt.get < 0) i = until
        i += 1
      }
      acnt.get + i
    }
    
    def loop_atomic_write = {
      var i = 0
      val until = totalwork / threadnum
      val acnt = atomic_cnt
      while (i < until) {
        acnt.set(i + cnt)
        i += 1
      }
      acnt.get + i
    }
    
    def loop_atomic_cas = {
      var i = 0
      val until = totalwork / threadnum
      val acnt = atomic_cnt
      while (i < until) {
        i += 1
        acnt.compareAndSet(i - 1, i)
      }
      acnt.get + i
    }
    
  }
  
}










