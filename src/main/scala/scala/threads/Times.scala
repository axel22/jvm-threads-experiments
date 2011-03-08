package scala.threads






import collection._







class Times(s: Settings) {
  import s._
  
  var totalTime: Long = _
  val runTimes = mutable.ArrayBuffer[Long]()
  val threadTimes = (0 until threadnum).map(_ => new mutable.ArrayBuffer[Long]).toArray
  val startTimes = (0 until threadnum).map(_ => new mutable.ArrayBuffer[Long]).toArray
  val appStartTime = System.nanoTime
  
  def measure[R](block: =>R) = {
    val start = System.nanoTime
    block
    val end = System.nanoTime
    end - start
  }
  
  def timeStamp = System.nanoTime - appStartTime
  
  private def fd(num: Double, divisor: Double, len: Int) = {
    val raw = (num / divisor).toString
    val (before, after) = raw.span(_ != '.')
    val s = before + after.take(5)
    if (s.length < len) s + (" " * (len - s.length))
    else s
  }
  
  private def ftind(thrind: Int) = if (thrind < 10) " " + thrind else thrind.toString
  
  private def formatTimes(fulltimes: Seq[Long], d: Double, numlast: Int) = if (fulltimes.nonEmpty) {
    val times = fulltimes.takeRight(numlast)
    val avg = fulltimes.foldLeft(0.0)(_ + _) / fulltimes.length
    val min = fulltimes.min
    val max = fulltimes.max
    times.foldLeft("")((prev, c) => prev + " " + fd(c, d, wdt)) +
    "(avg = " + fd(avg, d, wdt) +
    " min = " + fd(min, d, wdt) +
    " max = " + fd(max, d, wdt) +
    ")"
  }
  
  private def printTitle(title: String, numlast: Int) {
    println(title + " (showing last " + numlast + ")")
  }
  
  private def printThreadTimes(title: String, times: Seq[mutable.ArrayBuffer[Long]], d: Double, numlast: Int) = {
    printTitle(title, numlast)
    for (i <- 0 until threadnum) println("Thread " + ftind(i) + ": " + formatTimes(times(i), d, numlast))
  }
  
  def printAllTimes = {
    printTitle("Run times: ", lastshown)
    println(formatTimes(runTimes, div, lastshown))
    printThreadTimes("Threads times: ", threadTimes, div, lastshown)
    printThreadTimes("Thread start times: ", startTimes, div, lastshown)
    println("Total time: " + fd(totalTime, div, wdt))
  }
}
