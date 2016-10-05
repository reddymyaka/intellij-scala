package org.jetbrains.sbt.shell

import java.awt.event.KeyEvent
import java.io.File
import java.util

import com.intellij.execution.Executor
import com.intellij.execution.configurations.{GeneralCommandLine, JavaParameters}
import com.intellij.execution.console._
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, DefaultActionGroup}
import com.intellij.openapi.project.{DumbAwareAction, Project}
import com.intellij.openapi.projectRoots.{JavaSdkType, JdkUtil, Sdk, SdkTypeId}
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.sbt.project.structure.SbtRunner

/**
  * Created by jast on 2016-5-29.
  */
class SbtShellRunner(project: Project, consoleTitle: String, workingDir: String)
  extends AbstractConsoleRunnerWithHistory[LanguageConsoleImpl](project, consoleTitle, workingDir) {

  val sdk: Sdk = ProjectRootManager.getInstance(project).getProjectSdk
  assert(sdk != null)
  val sdkType: SdkTypeId = sdk.getSdkType
  assert(sdkType.isInstanceOf[JavaSdkType])
  val exePath: String = sdkType.asInstanceOf[JavaSdkType].getVMExecutablePath(sdk)
  // TODO get this from configuration
  val launcherJar: File = SbtRunner.getDefaultLauncher

  private val javaParameters: JavaParameters = new JavaParameters
  javaParameters.setJdk(sdk)
  javaParameters.configureByProject(project, 1, sdk)
  javaParameters.setWorkingDirectory(workingDir)
  javaParameters.setJarPath(launcherJar.getCanonicalPath)
  // TODO get from configuration
  javaParameters.getVMParametersList.addAll("-XX:MaxPermSize=128M", "-Xmx2G", "-Dsbt.log.noformat=true")

  private val myCommandLine: GeneralCommandLine = JdkUtil.setupJVMCommandLine(exePath, javaParameters, false)
  private val myConsoleView: LanguageConsoleImpl = {
    val cv = new LanguageConsoleImpl(project, SbtShellFileType.getName, SbtShellLanguage)
    cv.getConsoleEditor.setOneLineMode(true)
    cv
  }

  // lazy so that getProcessHandler will return something initialized when this is first accessed
  private lazy val myConsoleExecuteActionHandler: SbtShellExecuteActionHandler =
    new SbtShellExecuteActionHandler(getProcessHandler)


  override def createProcessHandler(process: Process): OSProcessHandler =
    new OSProcessHandler(process, myCommandLine.getCommandLineString)

  override def createConsoleView(): LanguageConsoleImpl = myConsoleView

  override def createProcess(): Process = myCommandLine.createProcess

  override def createExecuteActionHandler(): SbtShellExecuteActionHandler = {
    val historyController = new ConsoleHistoryController(SbtConsoleRootType, null, getConsoleView)
    historyController.install()

    myConsoleExecuteActionHandler
  }


  override def fillToolBarActions(toolbarActions: DefaultActionGroup,
                                  defaultExecutor: Executor,
                                  contentDescriptor: RunContentDescriptor): util.List[AnAction] = {

    val actions = super.fillToolBarActions(toolbarActions, defaultExecutor, contentDescriptor)
    val tabAction = createTabAction()
    actions.add(tabAction)
    actions
  }

  def createTabAction(): AnAction = {
    val upAction = new TabAction
    upAction.registerCustomShortcutSet(KeyEvent.VK_TAB, 0, null)
    upAction.getTemplatePresentation.setVisible(false)
    upAction
  }

  class TabAction extends DumbAwareAction {
    override def actionPerformed(e: AnActionEvent): Unit = {
      val text = getConsoleView.getEditorDocument.getText
      val history = getConsoleView.getHistoryViewer
      // TODO call code completion (ctrl+space by default)
    }
  }

  object SbtConsoleRootType extends ConsoleRootType("sbt.console", getConsoleTitle)
}
