package com.grburst.libtt.parser

import com.grburst.libtt.Player
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import spray.http.Uri

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

case class SearchParser(url: String = "/storage/emulated/0/mytischtennis.de/myclub-ajax.htm") {

  val browser = JsoupBrowser()
  val eventDoc = browser.parseFile(url)

  // val get: Future[List[Player]] = Future {

  //   val ttrTable = eventDoc >> element(".table-mytt") >> elementList("tr")
  //   val ttrData: List[Option[Player]] = ttrTable.map(x => (x >> elementList("td")).toList match {
  //     case List(r, d, n, c, t, s) => {
  //       val pId: Array[String] = (n >> attr("data-tooltipdata")("a")).split(";")
  //       val uri = Uri(c >> attr("href")("a"));

  //       Some(Player(pId(0).toInt, r.text, d.text.toIntOption, pId(2), c.text, uri.query.get("clubid").get.toInt, t.text.toIntOption))
  //     }
  //     case _ => None
  //   })

  //   ttrData.flatten

  // }

}
