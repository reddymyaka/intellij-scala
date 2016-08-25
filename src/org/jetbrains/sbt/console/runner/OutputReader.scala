// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console.runner

import java.io._
import java.util

object OutputReader {
  val FOUND: Boolean = true
  val END_OF_OUTPUT: Boolean = false
  private val BUFFER_SIZE: Int = 1024
}

class OutputReader(val output: Reader) extends FilterReader(output) {
  private val buffer: CyclicCharBuffer = new CyclicCharBuffer(OutputReader.BUFFER_SIZE)

  @throws[IOException]
  def waitForOutput(expected: String): Boolean = waitForOutput(util.Arrays.asList(expected), util.Arrays.asList[String]())

  @throws[IOException]
  def waitForOutput(expected: util.Collection[String], expectedExact: util.Collection[String]): Boolean = {
    var max: Int = 0
    import scala.collection.JavaConversions._
    for (s <- expected) {
      checkExpectedLength(s)
      max = Math.max(max, s.length)
    }
    var ch: Int = 0
    while (ch != -1) {
      ch = read
      buffer.append(ch.toChar)
      import scala.collection.JavaConversions._
      for (s <- expected) {
        if (buffer.contentEndsWith(s)) return OutputReader.FOUND
      }
      import scala.collection.JavaConversions._
      for (s <- expectedExact) {
        if (buffer.contentEquals(s)) return OutputReader.FOUND
      }
    }
    OutputReader.END_OF_OUTPUT
  }

  @throws[IOException]
  def skipBufferedOutput() {
    while (ready) skip(1)
  }

  def endOfOutputContains(expected: String): Boolean = {
    checkExpectedLength(expected)
    buffer.toString.contains(expected)
  }

  private def checkExpectedLength(expected: String) {
    if (expected.length > OutputReader.BUFFER_SIZE) throw new IllegalArgumentException("expected string is too long.")
  }
}
