package com.grburst.libtt.parser

import com.grburst.libtt.Event
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

// url = https://www.mytischtennis.de/community/events
case class EventsParser(url:String = "/storage/emulated/0/mytischtennis.de/events") {

  val browser = JsoupBrowser()
  val eventDoc = browser.parseFile(url)

  val currTTR = eventDoc >> element(".ttr-box") >> text("h3")

  def get():List[Event] = {
  // def get: Future[List[Event]] = Future {

    val ttrTable = eventDoc >> element("#tooltip-wrapper-ttr-table") >> element("tbody") >> elementList("tr")
    val ttrData: List[Option[Event]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(s,l,e,a,b,g,ty,tr,td) => {
        val eIdt:String = e >> attr("href")("a")
        val eId = eIdt.substring(eIdt.indexOf("(") + 1, eIdt.indexOf(",",eIdt.indexOf("(")))
        Some(Event(s.text,l.text,e.text,eId.toInt,a.text.toInt,b.text,g.text.replace(',', '.').toFloat,ty.text,tr.text.toIntOption,td.text.toIntOption))
      }
      case _ => None
    })

  ttrData.flatten

  }

}
