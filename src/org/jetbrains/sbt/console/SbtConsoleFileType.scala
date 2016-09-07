package org.jetbrains.sbt.console

import javax.swing.Icon

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.sbt.Sbt

/**
  * Dummy file type required by the sbt console LightVirtualFile
  */
object SbtConsoleFileType extends LanguageFileType(SbtConsoleLanguage) {

  override def getDefaultExtension: String = "sbtc"

  override def getName: String = "sbtConsole"

  override def getIcon: Icon = Sbt.FileIcon

  override def getDescription: String = "Sbt console file dummy"
}
