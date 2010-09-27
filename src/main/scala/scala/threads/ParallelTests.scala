package scala.threads




import collection._




object Settings {
  val totalwork = 1000000000
  val threadnum = 8
  val numtests = 20
}


object Times {
  var totalTime: Long = _
  val runTimes = mutable.ArrayBuffer[Long]()
  
  def measure[R](block: =>R) = {
    val start = System.nanoTime
    block
    val end = System.nanoTime
    end - start
  }
  
  def print = {
    println("Run times: " + runTimes.foldLeft("")(_ + " " + _ / 1000.0))
    println("Total time: " + (totalTime / 1000.0))
  }
}


object ParallelTest extends Application {
  import Settings._
  import Times._
  
  override def main(args: Array[String]) {
    println("Starting.")
    
    totalTime = measure {
      for (i <- 0 until numtests) test
    }
    
    println("Done.")
    Times.print
  }
  
  def test {
    val threads = for (i <- 0 until threadnum) yield new WorkerThread {
      override def run {
        loopWork
      }
    }
    
    runTimes += measure {
      threads foreach { _ start }
      threads foreach { _ join }
    }
  }
  
}


class WorkerThread extends Thread {
  import Settings._
  
  var flag: Boolean = false
  @volatile var vflag: Boolean = false
  
  def loopWork {
    var i = 0
    val until = totalwork / threadnum
    while (i < until) {
      flag = !flag
      i += 1
    }
  }
  
}















