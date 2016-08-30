// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.hackyconsole

import com.intellij.util.concurrency.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

class CompletionSignal {
  final private val done = new Semaphore
  final private val result: AtomicBoolean = new AtomicBoolean(false)

  def begin() {
    done.down
  }

  def success() {
    result.set(true)
  }

  def finished() {
    done.up
  }

  def waitForResult: Boolean = {
    done.waitFor
    result.get
  }
}
