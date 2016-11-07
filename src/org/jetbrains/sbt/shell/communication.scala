package org.jetbrains.sbt.shell

import java.io.{File, PrintWriter}
import java.net.Socket
import java.nio.charset.{Charset, StandardCharsets}

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.{DumbAwareAction, Project}

import scala.util.Try

/**
  * Created by jast on 2016-11-06.
  */
class SbtShellCommunication(project: Project) {

  // TODO: fetch all the setting/task/command symbols for use in autocompletion
  def fetchSymbols: List[String] = List.empty

  // TODO ask sbt to provide completions for a line via its parsers
  def completion(line: String): List[String] = List.empty

  /**
    * Execute an sbt task.
    */
  def task(task: String): Unit = withWriter { out => out.println(task) }

  private def getPort = {
    val file = project.getBaseDir.findChild("target").findChild("sbt-server-port")
    val content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8)
    content.toInt
  }

  private def withWriter(f: PrintWriter => Unit) = {
    val socket = Try(new Socket("localhost", getPort))
    for {
      s <- socket
      out = new PrintWriter(s.getOutputStream, true)
      res <- Try(f(out))
    } yield {
      res
    }
    socket.foreach(_.close())
  }
}
