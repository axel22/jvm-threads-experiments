



import collection._





object Tests {
  trait TestInfo {
    def classname: String
    def arguments: Seq[Seq[String]]
    def logfile = "testlog.txt"
    
    def defaults = Seq(Seq("logfile=" + logfile))
    def foreach[U](f: String => U) = rec_foreach(Seq(), arguments, f)
    private def rec_foreach[U](argsaccum: Seq[String], left: Seq[Seq[String]], f: String => U) {
      if (left.isEmpty) f(argsaccum.foldLeft("")(_ + " " + _))
      else for (argseq <- left(0)) rec_foreach(argsaccum ++ List(argseq), left.drop(1), f)
    }
  }
  
  object ThreadTests extends TestInfo {
    def classname = "scala.threads.ParallelTests"
    def arguments = Seq(
      Seq(
        // "loop_local_write",
        "loop_heap_read", 
        // "loop_heap_write",
        "loop_vread", 
        // "loop_vwrite",
        // "loop_atomic_read", 
        // "loop_atomic_write",
        // "loop_atomic_cas",
      ) map { "testname=" + _ },
      Seq(1, 2, 4, 6, 8) map { "threadnum=" + _ },
      Seq("totalwork=1000000000"),
      Seq("logging=true")
    ) ++ defaults
  }
  
  val map: Map[String, TestInfo] = mutable.Map() ++ (List(ThreadTests) map { t => (t.classname, t) })
  
  def apply(nm: String) = map(nm)
  
}
