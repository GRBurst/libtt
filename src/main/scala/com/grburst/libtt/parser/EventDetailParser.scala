package com.grburst.libtt.parser

import com.grburst.libtt.Match

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

// url = "https://www.mytischtennis.de/community/eventDetails?eventId=" + "144353353"
case class EventDetailParser(url: String = "/storage/emulated/0/mytischtennis.de/eventDetails.htm", id: String = "") {

  val browser = JsoupBrowser()
  val eventDoc = browser.parseFile(url + id)

  def get(): List[Match] = {
    val ttrTable = eventDoc >> element(".table-striped") >> element("tbody") >> elementList("tr")
    val ttrData: List[Option[Match]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(o, r, s1, s2, s3, s4, s5, s6, s7, g) => {
        val oIdt: Array[String] = (o >> attr("data-tooltipdata")("a")).split(";")
        val ottrt = o.text
        val ottr = ottrt.substring(ottrt.indexOf("(") + 1, ottrt.indexOf(")", ottrt.indexOf("(")))

        Some(Match(oIdt(2), ottr.toInt, oIdt(0).toInt, r.text, s1.text, s2.text, s3.text, s4.text, s5.text, s6.text, s7.text, g.text.replace(',', '.').toFloat))
      }
      case _ => None
    })

    ttrData.flatten

  }

}
