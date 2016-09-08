package org.jetbrains.sbt.console

import com.intellij.execution.configurations.{GeneralCommandLine, JavaParameters}
import com.intellij.execution.console._
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.{JavaSdkType, JdkUtil, Sdk, SdkTypeId}
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.sbt.project.structure.SbtRunner

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
  val launcherJar = SbtRunner.getDefaultLauncher

  val javaParameters: JavaParameters = new JavaParameters
  javaParameters.setJdk(sdk)
  javaParameters.configureByProject(project, 1, sdk)
  javaParameters.setWorkingDirectory(workingDir)
  javaParameters.setJarPath(launcherJar.getCanonicalPath)
  javaParameters.getVMParametersList.addAll("-XX:MaxPermSize=128M", "-Xmx2G")

  private val myCommandLine: GeneralCommandLine = JdkUtil.setupJVMCommandLine(exePath, javaParameters, false)

  override def createProcessHandler(process: Process): OSProcessHandler =
    new OSProcessHandler(process, myCommandLine.getCommandLineString)

  // this creates a LightVirtualFile in the background which is the basis for the console window
  // it's important that there is a FileTypeFactory for this language, so that the file gets handled correctly
  override def createConsoleView(): LanguageConsoleView =
    new LanguageConsoleImpl(project, "sbtConsole.sbtc", SbtConsoleLanguage)

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
