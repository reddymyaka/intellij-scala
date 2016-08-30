// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.hackyconsole

import org.apache.commons.lang.builder.EqualsBuilder
import java.io.File

import com.intellij.util.PathUtil

object SbtApplicationSettings {
  private val DEFAULT_SBT_VM_PARAMETERS: String = "-Xmx512M -XX:MaxPermSize=256M"
}

class SbtApplicationSettings {
  private var sbtLauncherJarPath: String = "" // Will use the bundled launcher;
  private var sbtLauncherVmParameters: String = SbtApplicationSettings.DEFAULT_SBT_VM_PARAMETERS
  private var useCustomJdk: Boolean = false
  private var jdkHome: String = null

  def getSbtLauncherJarPath: String = sbtLauncherJarPath

  def setSbtLauncherJarPath(sbtLauncherJarPath: String) {
    if (sbtLauncherJarPath.length == 0) this.sbtLauncherJarPath = ""
    else this.sbtLauncherJarPath = PathUtil.getCanonicalPath(new File(sbtLauncherJarPath).getCanonicalPath)
  }

  def getSbtLauncherVmParameters: String = sbtLauncherVmParameters

  def setSbtLauncherVmParameters(sbtLauncherVmParameters: String) {
    this.sbtLauncherVmParameters = sbtLauncherVmParameters
  }

  def isUseCustomJdk: Boolean = useCustomJdk

  def setUseCustomJdk(useCustomJdk: Boolean) {
    this.useCustomJdk = useCustomJdk
  }

  def getJdkHome: String = jdkHome

  def setJdkHome(jdkHome: String) {
    this.jdkHome = jdkHome
  }

  override def equals(obj: Any): Boolean = EqualsBuilder.reflectionEquals(this, obj)
}
