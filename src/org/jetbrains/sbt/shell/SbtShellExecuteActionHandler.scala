package org.jetbrains.sbt.shell

import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler
import com.intellij.execution.process.ProcessHandler

class SbtShellExecuteActionHandler(processHandler: ProcessHandler)
  extends ProcessBackedConsoleExecuteActionHandler(processHandler, true) {

}
