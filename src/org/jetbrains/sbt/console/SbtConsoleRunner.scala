package org.jetbrains.sbt.console

import com.intellij.execution.configurations.{GeneralCommandLine, JavaParameters}
import com.intellij.execution.console._
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.{JavaSdkType, JdkUtil, Sdk, SdkTypeId}
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.testFramework.LightVirtualFile

/**
  * Created by jast on 2016-5-29.
  */
class SbtConsoleRunner(project: Project, consoleTitle: String, workingDir: String)
  extends AbstractConsoleRunnerWithHistory[LanguageConsoleView](project, consoleTitle, workingDir) {

  val sdk: Sdk = ProjectRootManager.getInstance(project).getProjectSdk
  assert(sdk != null)
  val sdkType: SdkTypeId = sdk.getSdkType
  assert(sdkType.isInstanceOf[JavaSdkType])
  val exePath: String = sdkType.asInstanceOf[JavaSdkType].getVMExecutablePath(sdk)

  val javaParameters: JavaParameters = new JavaParameters
  javaParameters.setJdk(sdk)
  javaParameters.configureByProject(project, 1, sdk)

  private val myCommandLine: GeneralCommandLine = JdkUtil.setupJVMCommandLine(exePath, javaParameters, false)


  override def createProcessHandler(process: Process): OSProcessHandler =
    new OSProcessHandler(process, myCommandLine.getCommandLineString)

  override def createConsoleView(): LanguageConsoleView = {
    val file = new LightVirtualFile("sbtConsole.sbtc", SbtConsoleFileType, "")
    val helper = new LanguageConsoleImpl.Helper(project, file)
    new LanguageConsoleImpl(helper)
  }

  override def createProcess(): Process =
    myCommandLine.createProcess


  override def createExecuteActionHandler(): ProcessBackedConsoleExecuteActionHandler = {
    val handler: ProcessBackedConsoleExecuteActionHandler =
      new ProcessBackedConsoleExecuteActionHandler(getProcessHandler, false) {
        override def getEmptyExecuteAction: String = "sbt.console.execute"
      }
    val historyController = new ConsoleHistoryController(SbtConsoleRootType, null, getConsoleView)
    historyController.install()

    handler
  }

  object SbtConsoleRootType extends ConsoleRootType("sbt.console", getConsoleTitle)
}
