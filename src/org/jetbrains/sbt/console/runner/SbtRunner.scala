// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console.runner

import java.io._
import java.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil

object SbtRunner {
  private val PROMPT: String = "\n> "
  private val SCALA_PROMPT: String = "\nscala> "
  private val FAILED_TO_COMPILE_PROMPT: String = "Hit enter to retry or 'exit' to quit:"
  private val PROMPT_AFTER_EMPTY_ACTION: String = "> "
  private val ERROR_RUNNING_ACTION_PREFIX: String = "[error] Error running "
  private val ERROR_SBT_010_PREFIX: String = "[error] Total time:"
  private val LOG: Logger = Logger.getInstance("#org.jetbrains.sbt.console.runner.SbtRunner")

  private def getCommand(javaCommand: String, launcherJar: File, vmParameters: Array[String]): Array[String] = {
    val command: util.List[String] = new util.ArrayList[String]
    command.add(javaCommand)
    command.add("-Dsbt.log.noformat=true")
    command.add("-Djline.terminal=jline.UnsupportedTerminal")
    command.addAll(util.Arrays.asList(vmParameters: _*))
    command.addAll(util.Arrays.asList("-jar", launcherJar.getAbsolutePath))
    LOG.info("SBT command line: " + StringUtil.join(command, " "))
    command.toArray(new Array[String](command.size))
  }
}

class SbtRunner(val javaCommand: String, val workingDir: File, val launcherJar: File, val vmParameters: Array[String]) {
  if (!workingDir.isDirectory) throw new IllegalArgumentException("Working directory does not exist: " + workingDir)
  if (!launcherJar.isFile) throw new IllegalArgumentException("Launcher JAR file does not exist: " + launcherJar)
  command = SbtRunner.getCommand(javaCommand, launcherJar, vmParameters)
  sbt = new ProcessRunner(workingDir, command: _*)
  final private var sbt: ProcessRunner = null
  final private var command: Array[String] = null

  final def getFormattedCommand: String = {
    val sb: StringBuilder = new StringBuilder
    for (s <- command) {
      if (s.contains(" ")) sb.append("\"").append(s).append("\"")
      else sb.append(s)
      sb.append(" ")
    }
    sb.toString
  }

  def subscribeToOutput: OutputReader = sbt.subscribeToOutput

  @throws[IOException]
  def start(wait: Boolean) {
    // TODO: detect if the directory does not have a project
    val output: OutputReader = sbt.subscribeToOutput
    sbt.start()
    sbt.destroyOnShutdown()
    if (wait) output.waitForOutput(util.Arrays.asList(SbtRunner.PROMPT, SbtRunner.FAILED_TO_COMPILE_PROMPT), util.Arrays.asList[String]())
    output.close()
  }

  @throws[IOException]
  def start(wait: Boolean, onStarted: Runnable) {
    // TODO: detect if the directory does not have a project
    val output: OutputReader = sbt.subscribeToOutput
    sbt.start()
    sbt.destroyOnShutdown()
    if (wait) {
      output.waitForOutput(util.Arrays.asList(SbtRunner.PROMPT, SbtRunner.FAILED_TO_COMPILE_PROMPT), util.Arrays.asList[String]())
      output.close()
      onStarted.run()
    }
    else new Thread() {
      override def run() {
        try {
          output.waitForOutput(util.Arrays.asList(SbtRunner.PROMPT, SbtRunner.FAILED_TO_COMPILE_PROMPT), util.Arrays.asList[String]())
          output.close()
          onStarted.run()
        } catch {
          case e: IOException => // ignore
        }
      }
    }.start()
  }

  def destroy() {
    sbt.destroy()
  }

  def isAlive: Boolean = sbt.isAlive

  /**
    * @param action the SBT action to run, e.g. "compile"
    * @return false if an error was parsed from the output, true otherwise
    * @throws java.io.IOException
    */
  @throws[IOException]
  def execute(action: String): Boolean = {
    val output: OutputReader = sbt.subscribeToOutput
    try {
      sbt.writeInput(action + "\n")
      output.waitForOutput(util.Arrays.asList(SbtRunner.PROMPT, SbtRunner.SCALA_PROMPT, SbtRunner.FAILED_TO_COMPILE_PROMPT), util.Arrays.asList(SbtRunner.PROMPT_AFTER_EMPTY_ACTION))
    } finally output.close()
    val error: Boolean = output.endOfOutputContains(SbtRunner.ERROR_RUNNING_ACTION_PREFIX) || output.endOfOutputContains(SbtRunner.ERROR_SBT_010_PREFIX)
    SbtRunner.LOG.debug("completed: " + action + ", error: " + error)
    !error
  }
}
