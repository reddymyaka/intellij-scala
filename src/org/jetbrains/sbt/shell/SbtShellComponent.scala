package org.jetbrains.sbt.shell

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.{DumbAware, Project}
import com.intellij.openapi.startup.StartupManager

/**
  * Created by jast on 2016-5-30.
  */
class SbtShellComponent(var project: Project)
  extends AbstractProjectComponent(project) with DumbAware {

  // TODO running this on project open is maybe not optimally elegant
  override def projectOpened() {
    val manager = StartupManager.getInstance(myProject)

//    manager.registerPostStartupActivity(new SbtShellRunnable(project))
  }
}

object SbtShellComponent {
  def getInstance(project: Project): SbtShellComponent = project.getComponent(classOf[SbtShellComponent])
}
