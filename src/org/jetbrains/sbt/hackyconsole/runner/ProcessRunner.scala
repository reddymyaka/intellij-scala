package org.jetbrains.sbt.hackyconsole.runner

import java.io._

object ProcessRunner {

  private class DestroyProcessRunner(val process: Process) extends Runnable {
    def run() {
      process.destroy()
    }
  }

}

class ProcessRunner(val workingDir: File, val command: String*) {
  final private val outputMulticast: MulticastPipe = new MulticastPipe
  final private val builder: ProcessBuilder = new ProcessBuilder(command: _*)
  builder.directory(workingDir)
  builder.redirectErrorStream(true)

  private var process: Process = null
  private var shutdownHook: Thread = null
  private var input: Writer = null

  def subscribeToOutput: OutputReader = new OutputReader(outputMulticast.subscribe)

  @throws[IOException]
  def start() {
    process = builder.start
    shutdownHook = new Thread(new ProcessRunner.DestroyProcessRunner(process))
    val output: InputStreamReader = new InputStreamReader(new BufferedInputStream(process.getInputStream))
    val t: Thread = new Thread(new ReaderToWriterCopier(output, outputMulticast))
    t.setDaemon(true)
    t.start()
    input = new OutputStreamWriter(new BufferedOutputStream(process.getOutputStream))
  }

  def destroyOnShutdown() {
    Runtime.getRuntime.addShutdownHook(shutdownHook)
  }

  def destroy() {
    process.destroy()
    try
      process.waitFor

    catch {
      case e: InterruptedException => {
        e.printStackTrace()
      }
    }
    Runtime.getRuntime.removeShutdownHook(shutdownHook)
  }

  def isAlive: Boolean = {
    if (process == null) return false
    try {
      process.exitValue
      false
    } catch {
      case e: IllegalThreadStateException => {
        true
      }
    }
  }

  @throws[IOException]
  def writeInput(s: String) {
    input.write(s)
    input.flush()
  }
}
