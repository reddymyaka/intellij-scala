// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.hackyconsole

import java.io.{File, IOException, InputStream}
import java.util.Scanner

import com.intellij.openapi.application.{ApplicationManager, ModalityState, PathManager}
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.{DumbAware, DumbAwareRunnable, Project}
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.{FileUtil, StreamUtil}
import com.intellij.openapi.vfs._
import com.intellij.openapi.wm.{ToolWindowAnchor, ToolWindowManager}
import org.jetbrains.sbt.hackyconsole.runner._
import org.jetbrains.sbt.project.structure.SbtRunner


object SbtConsoleComponent {
  private val logger: Logger = Logger.getInstance(classOf[SbtConsoleComponent].getName)
  private val DEBUG: Boolean = false
  private val SBT_CONSOLE_TOOL_WINDOW_ID: String = "SBT Console"

  def getInstance(project: Project): SbtConsoleComponent = project.getComponent(classOf[SbtConsoleComponent])

  private def saveAllDocuments() {
    ApplicationManager.getApplication.invokeAndWait(new Runnable() {
      def run() {
        FileDocumentManager.getInstance.saveAllDocuments()
      }
    }, ModalityState.NON_MODAL)
  }
}

//noinspection ConvertNullInitializerToUnderscore,NameBooleanParameters
class SbtConsoleComponent(var project: Project)
  extends AbstractProjectComponent(project) with DumbAware {

  private var sbt: SbtConsoleRunner = null
  private var console: SbtConsole = null

  def executeInBackground(action: String): CompletionSignal = {
    val signal: CompletionSignal = new CompletionSignal
    signal.begin()
    val task = new Backgroundable(myProject, "Executing SBT Action", false) {
      override def run(indicator: ProgressIndicator) {
        try {
          SbtConsoleComponent.logger.debug("Begin executing: " + action)
          if (executeAndWait(action)) {
            signal.success()
            SbtConsoleComponent.logger.debug("Done executing: " + action)
          }
          else SbtConsoleComponent.logger.debug("Error executing: " + action)
        } catch {
          case e: IOException =>
            SbtConsoleComponent.logger.error(s"""Failed to execute action "$action". Maybe SBT failed to start?""", e)
        } finally signal.finished()
      }
    }
    queue(task)
    signal
  }

  override def projectOpened() {
    val manager = StartupManager.getInstance(myProject)
    manager.registerPostStartupActivity(new DumbAwareRunnable() {
      def run() {
        console = createConsole(project)
        registerToolWindow()
      }
    })
  }

  override def disposeComponent() {
    unregisterToolWindow()
    destroyProcess()
  }

  private def createConsole(project: Project): SbtConsole =
    new SbtConsole("SBT Action", project, this)

  private def registerToolWindow() {
    val toolWindowManager = ToolWindowManager.getInstance(myProject)
    if (toolWindowManager != null) {
      val toolWindow = toolWindowManager.registerToolWindow(SbtConsoleComponent.SBT_CONSOLE_TOOL_WINDOW_ID, false, ToolWindowAnchor.BOTTOM, myProject, true)
      val sbtRunnerComponent: SbtConsoleComponent = SbtConsoleComponent.getInstance(myProject)
      sbtRunnerComponent.getConsole.ensureAttachedToToolWindow(toolWindow, false)
    }
  }

  private def unregisterToolWindow() {
    val toolWindowManager = ToolWindowManager.getInstance(myProject)
    if (toolWindowManager != null && toolWindowManager.getToolWindow(SbtConsoleComponent.SBT_CONSOLE_TOOL_WINDOW_ID) != null) toolWindowManager.unregisterToolWindow(SbtConsoleComponent.SBT_CONSOLE_TOOL_WINDOW_ID)
  }

  private def queue(task: Backgroundable) {
    if (ApplicationManager.getApplication.isDispatchThread) task.queue()
    else ApplicationManager.getApplication.invokeAndWait(new Runnable() {
      def run() {
        task.queue()
      }
    }, ModalityState.NON_MODAL)
  }

  /**
    * @param action the SBT action to run
    * @return false if an error was detected, true otherwise
    */
  @throws[IOException]
  def executeAndWait(action: String): Boolean = {
    SbtConsoleComponent.saveAllDocuments()
    if (!startIfNotStartedSafe(true)) return false
    var success: Boolean = false
    try
      success = sbt.execute(action)
    // TODO: update target folders (?)
    // org.jetbrains.idea.maven.project.MavenProjectsManager#updateProjectFolders
    // org.jetbrains.idea.maven.execution.MavenRunner#runBatch
    // org.jetbrains.idea.maven.execution.MavenRunner#updateTargetFolders

    catch {
      case e: IOException =>
        destroyProcess()
        throw e
    }
    VirtualFileManager.getInstance.refreshWithoutFileWatcher(true)
    success
  }

  final def getConsole: SbtConsole = console

  final def getFormattedCommand: String = sbt.getFormattedCommand

  final def startIfNotStartedSafe(wait: Boolean): Boolean =
    try {
      startIfNotStarted(wait)
      true
    } catch {
    case e: Throwable =>
      val toolWindowId: String = "SBT Console"
//      ToolWindowManager.getInstance(project).notifyByBalloon(toolWindowId, MessageType.ERROR, s"Unable to start SBT. ${e.getMessage}")
      SbtConsoleComponent.logger.info("Failed to start SBT", e)
      val e2 = e
      false
    }

  @throws[IOException]
  private def startIfNotStarted(wait: Boolean) {
    if (!isSbtAlive) {
      sbt = new SbtConsoleRunner("java", projectDir, launcherJar, vmParameters)
      printToMessageWindow()
      if (SbtConsoleComponent.DEBUG) printToLogFile()
      sbt.start(wait, new Runnable() {
        def run() {
          try
            // See https://github.com/orfjackal/idea-sbt-plugin/issues/49
            sbt.execute("""eval {System.setProperty("jline.terminal" , "none"); "<modified system property 'jline.terminal' for Scala console compatibility>"}""")

          catch {
            case e: Exception =>
              // ignore
          }
        }
      })
    }
  }

  final def isSbtAlive: Boolean = sbt != null && sbt.isAlive

  private def projectDir: File = {
    val baseDir = myProject.getBaseDir
    assert(baseDir != null)
    new File(baseDir.getPath)
  }

  private def launcherJar: File = {
    SbtRunner.getDefaultLauncher
  }

  @throws[IOException]
  private def unpackBundledLauncher: File = {
    val launcherName: String = "sbt-launch.jar"
    val launcherTemp: File = new File(new File(PathManager.getSystemPath, "sbt"), launcherName)
    if (!launcherTemp.exists) {
      val resource: InputStream = classOf[SbtConsoleComponent].getClassLoader.getResourceAsStream("sbt-launch.jar")
      val bytes: Array[Byte] = StreamUtil.loadFromStream(resource)
      FileUtil.writeToFile(launcherTemp, bytes)
    }
    launcherTemp
  }

  private def vmParameters: Array[String] =
    new Array[String](0)

  private def printToMessageWindow() {
    // org.jetbrains.idea.maven.execution.MavenExecutor#myConsole
    val process: SbtProcessHandler = new SbtProcessHandler(this, sbt.subscribeToOutput)
    console.attachToProcess(process, this)
    process.startNotify()
  }

  private def printToLogFile() {
    val output: OutputReader = sbt.subscribeToOutput
    val t: Thread = new Thread(new Runnable() {
      def run() {
        val scanner: Scanner = new Scanner(output)
        while (scanner.hasNextLine) SbtConsoleComponent.logger.info(scanner.nextLine)
      }
    })
    t.setDaemon(true)
    t.start()
  }

  def destroyProcess() {
    if (sbt != null) {
      sbt.destroy()
      sbt = null
    }
  }
}
