// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console

import com.intellij.openapi.components._
import org.jetbrains.annotations.NotNull

@State(name = "SbtSettings", storages = Array(new Storage(id = "default", file = "$APP_CONFIG$/other.xml")))
class SbtApplicationSettingsComponent extends PersistentStateComponent[SbtApplicationSettings] with ApplicationComponent {
  private var applicationSettings: SbtApplicationSettings = new SbtApplicationSettings

  def getState: SbtApplicationSettings = applicationSettings

  def loadState(state: SbtApplicationSettings) {
    applicationSettings = state
  }

  def initComponent() {
  }

  def disposeComponent() {
  }

  @NotNull def getComponentName: String = "SbtSettings"
}
