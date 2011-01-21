package scala.threads




import collection._






object ParallelTests extends Test {
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
        reset
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
    val threads = for (i <- 0 until threadnum) yield new WorkerThread(times, settings) {
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

  val chmap = new java.util.concurrent.ConcurrentHashMap[Int, Int]
  val cskip = new java.util.concurrent.ConcurrentSkipListMap[Int, Int]
  val hmap = new java.util.HashMap[Int, Int]
  
  def reset {
    chmap.clear
    cskip.clear
    hmap.clear
  }
  
  class WorkerThread(times: Times, settings: Settings) extends Thread {
    import times._
    import settings._
    import java.util.concurrent.atomic.AtomicInteger
    
    var cnt: Int = 0
    @volatile var vcnt: Int = 0
    val atomic_cnt = new AtomicInteger(0)
    val atomic_tlocal_cnt = new java.lang.ThreadLocal[java.util.concurrent.atomic.AtomicInteger] {
      override def initialValue = new AtomicInteger(0)
    }
    val hmap = new java.util.HashMap[Int, Int]
    val totalMapConstructions = 10
    
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
      case "loop_atomic_tlocal_cas" => loop_atomic_tlocal_cas
      case "loop_atomic_weakcas" => loop_atomic_weakcas
      case "loop_atomic_tlocal_weakcas" => loop_atomic_tlocal_weakcas
      case "conchashmap_insert" => conchashmap_insert
      case "concskiplist_insert" => concskiplist_insert
      case "linear_insert" => linear_insert
      case "currthread" => currthread
      case "threadlocal" => threadlocal
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
    
    def loop_atomic_tlocal_cas = {
      var i = 0
      val until = totalwork / threadnum
      val acnt = atomic_tlocal_cnt.get
      while (i < until) {
        i += 1
        acnt.compareAndSet(i - 1, i)
      }
      acnt.get + i
    }
    
    def loop_atomic_weakcas = {
      var i = 0
      val until = totalwork / threadnum
      val acnt = atomic_cnt
      while (i < until) {
        i += 1
        acnt.weakCompareAndSet(i - 1, i)
      }
      acnt.get + i
    }
    
    def loop_atomic_tlocal_weakcas = {
      var i = 0
      val until = totalwork / threadnum
      val acnt = atomic_tlocal_cnt.get
      while (i < until) {
        i += 1
        acnt.weakCompareAndSet(i - 1, i)
      }
      acnt.get + i
    }
    
    def conchashmap_insert = {
      var i = 0
      val until = totalwork / threadnum
      val hm = chmap
      while (i < until) {
        i += 1
        hm.put(i, i)
      }
    }
    
    def concskiplist_insert = {
      var i = 0
      val until = totalwork / threadnum
      val cs = cskip
      while (i < until) {
        i += 1
        cs.put(i, i)
      }
    }
    
    def linear_insert = {
      var i = 0
      val until = totalwork / threadnum
      val hm = hmap
      while (i < until) {
        i += 1
        hm.put(i, i)
      }
    }
    
    def currthread = {
      var i = 0
      val until = totalwork / threadnum
      while (i < until) {
        if (Thread.currentThread() eq null) i = until + i + 1
        i += 1
      }
      if (i > until) println("current thread was null " + i)
    }
    
    def threadlocal = {
      var i = 0
      val until = totalwork / threadnum
      while (i < until) {
        if (atomic_tlocal_cnt.get eq null) i = until + i + 1
        i += 1
      }
      if (i > until) println("thread local value was null " + i)
    }
    
  }
  
}










