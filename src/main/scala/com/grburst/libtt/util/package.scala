package com.grburst.libtt.util

import scala.util.Try

package object types {
  type TTR = Option[Int]
  type Rank = Option[Int]
  type HttpDoc = net.ruippeixotog.scalascraper.model.Document
}

package object parsingHelper {

  implicit class StringHelper(s: String) {
    def toIntOption: Option[Int] = Try(s.toInt).toOption
  }

  def caseClassToMap(cc: Option[AnyRef]) = {
    if (cc.isDefined) {
      (Map[String, Any]() /: cc.get.getClass.getDeclaredFields) { (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc.get))
      }
    } else
      Map[String, Any]()
  }

}
