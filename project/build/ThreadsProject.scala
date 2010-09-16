
import sbt._




class ThreadsProject(info: ProjectInfo) extends DefaultProject(info) {
  
  
  // actions
  lazy val runserver = task {
    "ls" !
  }
  
}


