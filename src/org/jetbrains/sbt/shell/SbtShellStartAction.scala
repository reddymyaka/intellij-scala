package org.jetbrains.sbt.shell

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, PlatformDataKeys}
import com.intellij.openapi.ui.Messages

/**
  * Created by jast on 2016-11-04.
  */
class SbtShellStartAction extends AnAction("Run SBT Shell") {
  override def actionPerformed(event: AnActionEvent): Unit = {
    SbtShellRunner.run(event.getProject)
  }
}
