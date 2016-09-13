package org.jetbrains.sbt.console

import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler
import com.intellij.execution.process.ProcessHandler

/** Default: Enter key */
class EnterActionHandler(processHandler: ProcessHandler)
  extends ProcessBackedConsoleExecuteActionHandler(processHandler, true)

/** Autocompletion: Tab key */
class TabActionHandler(processHandler: ProcessHandler)
  extends ProcessBackedConsoleExecuteActionHandler(processHandler, true) {

  override def processLine(line: String): Unit = sendText(line + "\t")
}