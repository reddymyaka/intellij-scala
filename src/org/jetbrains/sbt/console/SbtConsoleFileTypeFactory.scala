package org.jetbrains.sbt.console

import com.intellij.openapi.fileTypes.{FileTypeConsumer, FileTypeFactory}

/**
  * Created by jast on 2016-09-07.
  */
class SbtConsoleFileTypeFactory extends FileTypeFactory {
  override def createFileTypes(consumer: FileTypeConsumer): Unit =
    consumer.consume(SbtConsoleFileType, "sbtc")
}
