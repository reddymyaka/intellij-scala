// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console

import java.awt._
import java.util
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing._

import com.intellij.execution.filters._
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.{ProcessAdapter, ProcessEvent, ProcessHandler}
import com.intellij.execution.ui.{ConsoleView, ConsoleViewContentType}
import com.intellij.openapi.actionSystem._
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.{DumbAwareAction, Project}
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.{IconLoader, Key}
import com.intellij.openapi.wm.{ToolWindow, ToolWindowManager}
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.content.{Content, ContentFactory}

object SbtConsole {
  // org.jetbrains.idea.maven.embedder.MavenConsoleImpl
  private val logger: Logger = Logger.getInstance(classOf[SbtConsole].getName)
  private val CONSOLE_KEY: Key[SbtConsole] = Key.create("SBT_CONSOLE_KEY")
  val CONSOLE_FILTER_REGEXP: String = "\\s" + RegexpFilter.FILE_PATH_MACROS + ":" + RegexpFilter.LINE_MACROS + ":\\s"

  private def createConsoleView(project: Project): ConsoleView  = {
    // TODO can we figure out how to make this a LanguageConsole with IDEA 14.1+
    //      We need that for console history
    val consoleView = createTextConsole(project)
    addFilters(project, consoleView)
    consoleView
  }

  private def createTextConsole(project: Project): ConsoleView = {
    val builder = TextConsoleBuilderFactory.getInstance.createBuilder(project)
    val exceptionFilter = new ExceptionFilter(GlobalSearchScope.allScope(project))
    val regexpFilter = new RegexpFilter(project, CONSOLE_FILTER_REGEXP)
    import scala.collection.JavaConversions._
    for (filter <- util.Arrays.asList(exceptionFilter, regexpFilter)) {
      builder.addFilter(filter)
    }
    builder.getConsole
  }

  private def addFilters(project: Project, consoleView: ConsoleView) {
    consoleView.addMessageFilter(new ExceptionFilter(GlobalSearchScope.allScope(project)))
    consoleView.addMessageFilter(new RegexpFilter(project, CONSOLE_FILTER_REGEXP))
  }
}

//noinspection NameBooleanParameters
class SbtConsole(val title: String, val project: Project, val runnerComponent: SbtConsoleComponent) {
  private val consoleView: ConsoleView = SbtConsole.createConsoleView(project)
  private val isOpen: AtomicBoolean = new AtomicBoolean(false)
  private var finished: Boolean = false

  def isFinished: Boolean = finished

  def finish() {
    finished = true
  }

  def attachToProcess(processHandler: ProcessHandler, runnerComponent: SbtConsoleComponent) {
    consoleView.print(runnerComponent.getFormattedCommand + "\n\n", ConsoleViewContentType.SYSTEM_OUTPUT)
    consoleView.attachToProcess(processHandler)
    processHandler.addProcessListener(new ProcessAdapter {
      override def onTextAvailable(event: ProcessEvent, outputType: Key[_]) {
        ApplicationManager.getApplication.invokeLater(new Runnable() {
          def run() {
            val window = ToolWindowManager.getInstance(project).getToolWindow(MessageBundle.message("sbt.console.id"))
            /* When we retrieve a window from ToolWindowManager before SbtToolWindowFactory is called,
                                     * we get an undesirable Content */
            for (each <- window.getContentManager.getContents) {
              if (each.getUserData(SbtConsole.CONSOLE_KEY) == null) window.getContentManager.removeContent(each, false)
            }
            ensureAttachedToToolWindow(window, true)
          }
        })
      }

      override def processTerminated(event: ProcessEvent) {
        finish()
      }
    })
  }

  final def ensureAttachedToToolWindow(window: ToolWindow, activate: Boolean) {
    if (!isOpen.compareAndSet(false, true)) return
    attachToToolWindow(window)
    if (activate) if (!window.isActive) window.activate(null, false)
  }

  def attachToToolWindow(window: ToolWindow) {
    // org.jetbrains.idea.maven.embedder.MavenConsoleImpl#ensureAttachedToToolWindow
    val toolWindowPanel = new SimpleToolWindowPanel(false, true)
    val consoleComponent: JComponent = consoleView.getComponent
    toolWindowPanel.setContent(consoleComponent)
    val startSbtAction: SbtConsole#StartSbtAction = new StartSbtAction
    toolWindowPanel.setToolbar(createToolbar(startSbtAction))
    startSbtAction.registerCustomShortcutSet(CommonShortcuts.getRerun, consoleComponent)
    val content = ContentFactory.SERVICE.getInstance.createContent(toolWindowPanel, title, true)
    content.putUserData(SbtConsole.CONSOLE_KEY, this)
    window.getContentManager.addContent(content)
    window.getContentManager.setSelectedContent(content)
    removeUnusedTabs(window, content)
  }

  private def createToolbar(startSbtAction: AnAction): JComponent = {
    val toolbarPanel: JPanel = new JPanel(new GridLayout)
    val group = new DefaultActionGroup
    val killSbtAction = new KillSbtAction
    group.add(startSbtAction)
    group.add(killSbtAction)
    // Adds "Next/Prev hyperlink", "Use Soft Wraps", and "Scroll to End"
    val actions = consoleView.createConsoleActions
    for (action <- actions) {
      group.add(action)
    }
    toolbarPanel.add(ActionManager.getInstance.createActionToolbar("SbtConsoleToolbar", group, false).getComponent)
    toolbarPanel
  }

  private def removeUnusedTabs(window: ToolWindow, content: Content) {
    for (each <- window.getContentManager.getContents) {
      lazy val console: SbtConsole = each.getUserData(SbtConsole.CONSOLE_KEY)
      if (each.isPinned || each == content || console == null || title != console.title) { /*continue*/ }
      else if (console.isFinished) window.getContentManager.removeContent(each, false)
    }
  }

  def scrollToEnd() {
    consoleView.asInstanceOf[ConsoleViewImpl].scrollToEnd()
  }

  private class StartSbtAction() extends DumbAwareAction("Start SBT", "Start SBT", IconLoader.getIcon("/toolwindows/toolWindowRun.png")) {
    override def actionPerformed(event: AnActionEvent) {
      runnerComponent.startIfNotStartedSafe(false)
    }

    override def update(event: AnActionEvent) {
      event.getPresentation.setEnabled(!runnerComponent.isSbtAlive)
    }
  }

  private class KillSbtAction() extends DumbAwareAction("Kill SBT", "Forcibly kill the SBT process", IconLoader.getIcon("/debugger/killProcess.png")) {
    override def actionPerformed(event: AnActionEvent) {
      runnerComponent.destroyProcess()
    }

    override def update(event: AnActionEvent) {
      event.getPresentation.setEnabled(runnerComponent.isSbtAlive)
    }
  }

}
