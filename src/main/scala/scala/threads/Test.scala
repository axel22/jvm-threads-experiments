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


    // val props = new Properties
    // props.put("mail.smtp.host", "benchmark-report-server")
    // props.put("mail.from", "reports@benchmark.report")

    // Session session = Session.getInstance(props, null);

    // try {
    //   val mimemsg = new MimeMessage(session)
    //   msg.setFrom()
    //   msg.setRecipients(Message.RecipientType.TO, settings.email)
    //   msg.setSubject("Benchmark reports ")
    //   msg.setSentDate(new Date());
    //   msg.setText("Hello, world!\n");
    //   Transport.send(msg);
    // } catch (MessagingException mex) {
    //   System.out.println("send failed, exception: " + mex);
    // }    




