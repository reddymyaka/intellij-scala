package org.jetbrains.sbt.console

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.{DumbAware, DumbAwareRunnable, Project}
import com.intellij.openapi.startup.StartupManager

/**
  * Created by jast on 2016-5-30.
  */
class SbtConsoleComponent(var project: Project)
  extends AbstractProjectComponent(project) with DumbAware {


  override def projectOpened() {
    val manager = StartupManager.getInstance(myProject)
    val title = SbtConsoleComponent.SBT_CONSOLE_TOOL_WINDOW_ID

    manager.registerPostStartupActivity(
      new DumbAwareRunnable() {
        def run() {
          val cr = new SbtConsoleRunner(project, title, project.getBaseDir.getCanonicalPath)
          cr.initAndRun()
        }
      })
  }

}


object SbtConsoleComponent {
  private val logger: Logger = Logger.getInstance(classOf[SbtConsoleComponent].getName)
  private val SBT_CONSOLE_TOOL_WINDOW_ID: String = "SBT Console"

  def getInstance(project: Project): SbtConsoleComponent = project.getComponent(classOf[SbtConsoleComponent])

}
