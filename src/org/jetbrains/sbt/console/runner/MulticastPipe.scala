// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console.runner

import java.io._
import java.util
import java.util.concurrent.CopyOnWriteArrayList

class MulticastPipe extends Writer {
  final private val subscribers: util.List[PipedWriter] = new CopyOnWriteArrayList[PipedWriter]

  def subscribe: Reader = try {
    val r: PipedReader = new PipedReader
    subscribers.add(new PipedWriter(r))
    r

  } catch {
    case e: IOException =>
      throw new RuntimeException(e)
  }

  private def unsubscribe(w: PipedWriter) {
    subscribers.remove(w)
  }

  @throws[IOException]
  def write(cbuf: Array[Char], off: Int, len: Int) {
    import scala.collection.JavaConversions._
    for (w <- subscribers) {
      try {
        w.write(cbuf, off, len)
        w.flush()
      } catch {
        case e: IOException =>
          unsubscribe(w)
      }
    }
  }

  @throws[IOException]
  def flush() {
    import scala.collection.JavaConversions._
    for (w <- subscribers) {
      try
        w.flush()

      catch {
        case e: IOException =>
          unsubscribe(w)
      }
    }
  }

  @throws[IOException]
  def close() {
    import scala.collection.JavaConversions._
    for (w <- subscribers) {
      try
        w.close()

      catch {
        case e: IOException =>
          unsubscribe(w)
      }
    }
  }
}
