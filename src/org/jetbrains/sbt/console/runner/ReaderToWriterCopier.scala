// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console.runner

import java.io._

class ReaderToWriterCopier(val source: Reader, val target: Writer) extends Runnable {
  def run() {
    try {
      val buf: Array[Char] = new Array[Char](1024)
      var len: Int = 0
      while ( {
        len = source.read(buf)
        len
      } != -1)
        target.write(buf, 0, len)
    }
    catch {
      case e: IOException =>
        e.printStackTrace()
    } finally try
      target.close()

    catch {
      case e: IOException =>
        e.printStackTrace()
    }
  }
}
