package org.jetbrains.sbt.shell

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.{DumbAware, DumbAwareRunnable, Project}
import com.intellij.openapi.startup.StartupManager

/**
  * Created by jast on 2016-5-30.
  */
class SbtShellComponent(var project: Project)
  extends AbstractProjectComponent(project) with DumbAware {


  override def projectOpened() {
    val manager = StartupManager.getInstance(myProject)
    val title = SbtShellComponent.SBT_SHELL_TOOL_WINDOW_ID

    manager.registerPostStartupActivity(
      new DumbAwareRunnable() {
        def run() {
          val cr = new SbtShellRunner(project, title, project.getBaseDir.getCanonicalPath)
          cr.initAndRun()
        }
      })
  }

}


object SbtShellComponent {
  private val logger: Logger = Logger.getInstance(classOf[SbtShellComponent].getName)
  private val SBT_SHELL_TOOL_WINDOW_ID: String = "SBT Shell"

  def getInstance(project: Project): SbtShellComponent = project.getComponent(classOf[SbtShellComponent])

}
