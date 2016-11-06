package org.jetbrains.sbt.shell

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}
import org.jetbrains.plugins.scala.icons.Icons

/**
  * Created by jast on 2016-11-04.
  */
class SbtShellStartAction extends AnAction("Run SBT Shell") {

  override def update(e: AnActionEvent): Unit =
    e.getPresentation.setIcon(Icons.SCALA_CONSOLE)

  override def actionPerformed(event: AnActionEvent): Unit = {
    SbtShellRunner.run(event.getProject)
  }
}
