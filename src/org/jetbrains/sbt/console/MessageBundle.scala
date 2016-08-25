// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0
package org.jetbrains.sbt.console

import com.intellij.CommonBundle
import java.lang.ref._
import java.util.ResourceBundle

object MessageBundle {
  private val BUNDLE: String = "MessageBundle"
  private var ourBundle: Reference[ResourceBundle] = null

  def message(key: String, params: Any*): String = CommonBundle.message(getBundle, key, params)

  private def getBundle: ResourceBundle = {
    var bundle: ResourceBundle = null
    if (ourBundle != null) bundle = ourBundle.get
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE)
      ourBundle = new SoftReference[ResourceBundle](bundle)
    }
    bundle
  }
}

class MessageBundle private() {
}
