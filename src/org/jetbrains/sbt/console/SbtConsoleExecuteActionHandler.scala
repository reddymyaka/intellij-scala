package org.jetbrains.sbt.console

import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler
import com.intellij.execution.process.ProcessHandler

/** Default: Enter key */
class SbtConsoleExecuteActionHandler(processHandler: ProcessHandler)
  extends ProcessBackedConsoleExecuteActionHandler(processHandler, true) {

  override def getEmptyExecuteAction: String = "sbt.console.execute"

  def sendTab(line: String): Unit =
    sendText(line + "\t")
}
