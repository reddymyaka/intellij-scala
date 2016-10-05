package org.jetbrains.sbt.shell

import com.intellij.codeInsight.completion._
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
  * Created by jast on 2016-09-28.
  */
class SbtShellCompletionContributor extends CompletionContributor {

  extend(
    CompletionType.BASIC,
    PlatformPatterns.psiElement().withLanguage(SbtShellLanguage),
    SbtShellCompletionProvider)

}

object SbtShellCompletionProvider extends CompletionProvider[CompletionParameters] {
  override def addCompletions(parameters: CompletionParameters,
                              context: ProcessingContext,
                              result: CompletionResultSet): Unit = {
    val elem = LookupElementBuilder.create("dingsbums")
    result.addElement(elem)
  }

}