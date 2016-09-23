package com.grburst.libtt.util

import scala.util.Try

package object parsingHelper {

  implicit class StringHelper(s: String) {
    def toIntOption: Option[Int] = Try(s.toInt).toOption
  }

}
