// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console

import com.intellij.execution.process._
import com.intellij.openapi.diagnostic.Logger
import java.io._

import org.jetbrains.sbt.console.runner.OutputReader


object SbtProcessHandler {
  private val logger = Logger.getInstance(classOf[SbtProcessHandler].getName)

  private class NotifyWhenTextAvailable(val process: SbtProcessHandler, val output: Reader) extends Runnable {
    def run() {
      try {
        val cbuf: Array[Char] = new Array[Char](100)
        var len: Int = 0
        while ( {
          len = output.read(cbuf); len
        } != -1) {
          val text: String = new String(cbuf, 0, len)
          val withoutCr: String = text.replace("\r", "")
          process.notifyTextAvailable(withoutCr, ProcessOutputTypes.STDOUT)
        }

      } catch {
        case e: IOException =>
          logger.error(e)
      } finally process.notifyProcessTerminated(0)
    }
  }

  private class ExecuteUserEnteredActions(val sbt: SbtConsoleComponent) extends OutputStream {
    final private val commandBuffer: StringBuilder = new StringBuilder

    def write(b: Int) {
      val ch: Char = b.toChar
      if (ch == '\n') sbt.executeInBackground(buildCommand)
      else commandBuffer.append(ch)
    }

    private def buildCommand: String = {
      val command: String = commandBuffer.toString.trim
      commandBuffer.setLength(0)
      command
    }
  }

}

class SbtProcessHandler(val sbt: SbtConsoleComponent, val output: OutputReader) extends ProcessHandler {
  override def startNotify() {
    val outputNotifier: SbtProcessHandler.NotifyWhenTextAvailable = new SbtProcessHandler.NotifyWhenTextAvailable(this, output)
    addProcessListener(new ProcessAdapter() {
      override def startNotified(event: ProcessEvent) {
        val t: Thread = new Thread(outputNotifier)
        t.setDaemon(true)
        t.start()
      }
    })
    super.startNotify()
  }

  def getProcessInput: OutputStream = new SbtProcessHandler.ExecuteUserEnteredActions(sbt)

  protected def destroyProcessImpl() {
    sbt.destroyProcess()
  }

  protected def detachProcessImpl() {
    throw new UnsupportedOperationException("SBT cannot be detached")
  }

  def detachIsDefault: Boolean = false
}
