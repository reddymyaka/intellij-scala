// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console

import com.intellij.openapi.components._
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import java.io.File

import org.jetbrains.sbt.project.settings.SbtProjectSettings

@State(name = "SbtSettings", storages = Array(new Storage(id = "default", file = "$PROJECT_FILE$", scheme = StorageScheme.DEFAULT), new Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)))
class SbtProjectSettingsComponent(val project: Project)
  extends AbstractProjectComponent(project) with PersistentStateComponent[SbtProjectSettings] {

  private var projectSettings: SbtProjectSettings = new SbtProjectSettings

  def getState: SbtProjectSettings = projectSettings

  def loadState(state: SbtProjectSettings) {
    this.projectSettings = state
  }

  def effectiveSbtLauncherVmParameters(applicationSettings: SbtApplicationSettingsComponent): String =
    if (projectSettings.isUseApplicationSettings) applicationSettings.getState.getSbtLauncherVmParameters
    else getState.getSbtLauncherVmParameters

  def effectiveSbtLauncherJarPath(applicationSettings: SbtApplicationSettingsComponent): String = if (projectSettings.isUseApplicationSettings) applicationSettings.getState.getSbtLauncherJarPath
  else getState.getSbtLauncherJarPath

  def getJavaCommand(applicationSettings: SbtApplicationSettingsComponent): String = if (applicationSettings.getState.isUseCustomJdk) {
    val systemDependentJdkHome: String = FileUtil.toSystemDependentName(applicationSettings.getState.getJdkHome)
    systemDependentJdkHome + File.separator + "bin" + File.separator + "java"
  }
  else "java"
}
