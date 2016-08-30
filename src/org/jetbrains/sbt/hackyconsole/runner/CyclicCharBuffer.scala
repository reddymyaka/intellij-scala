// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.hackyconsole.runner

class CyclicCharBuffer(val capacity: Int) {
  buffer = new Array[Char](capacity)
  final private var buffer: Array[Char] = null
  private var start: Int = 0
  private var length: Int = 0

  def charAt(i: Int): Char = buffer(index(i))

  private def index(i: Int): Int = (start + i) % buffer.length

  def append(c: Char) {
    if (isFull) removeFirst()
    insertLast(c)
  }

  private def isFull: Boolean = length == buffer.length

  private def removeFirst() {
    start += 1
    length -= 1
  }

  private def insertLast(c: Char) {
    buffer(index(length)) = c
    length += 1
  }

  def contentEquals(that: String): Boolean = this.length == that.length && contentEndsWith(that)

  def contentEndsWith(that: String): Boolean = {
    if (this.length < that.length) return false
    var i: Int = 0
    while (i < that.length) {
      {
        val j: Int = this.length - that.length + i
        if (this.charAt(j) != that.charAt(i)) return false
      }
      {
        i += 1; i - 1
      }
    }
    true
  }

  override def toString: String = {
    val s: StringBuilder = new StringBuilder(buffer.length)
    var i: Int = 0
    while (i < length) {
      {
        s.append(charAt(i))
      }
      {
        i += 1; i - 1
      }
    }
    s.toString
  }
}
