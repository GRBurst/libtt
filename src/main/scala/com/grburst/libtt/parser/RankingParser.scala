package com.grburst.libtt.parser

import com.grburst.libtt.Player
import com.grburst.libtt.util.parsingHelper.StringHelper

import spray.http.Uri

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

// val urlPattern = "http://www.mytischtennis.de/community/ajax/_rankingList?kontinent=Europa&land=DE&deutschePlusGleichgest=no&alleSpielberechtigen=&verband=&bezirk=&kreis=&regionPattern123=&regionPattern4=&regionPattern5=&geschlecht=&geburtsJahrVon=&geburtsJahrBis=&ttrVon=&ttrBis=&ttrQuartalorAktuell=aktuell&anzahlErgebnisse=100&vorname=&nachname=&verein=&vereinId=&vereinPersonenSuche=&vereinIdPersonenSuche=&ligen=&groupId=&showGroupId=&deutschePlusGleichgest2=no&ttrQuartalorAktuell2=aktuell&showmyfriends=0'"
case class RankingParser(url: String = "/storage/emulated/0/mytischtennis.de/myclub-ajax.htm") {

  val browser = JsoupBrowser()
  val eventDoc = browser.parseFile(url)

  def get(): List[Player] = {

    val ttrTable = eventDoc >> element(".table-mytt") >> elementList("tr")
    val ttrData: List[Option[Player]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(r, d, n, c, t, s) => {
        val pId: Array[String] = (n >> attr("data-tooltipdata")("a")).split(";")
        val uri = Uri(c >> attr("href")("a"));

        Some(Player(pId(0).toInt, r.text, d.text.toIntOption.getOrElse(-1), pId(2), c.text, uri.query.get("clubid").get.toInt, t.text.toIntOption.getOrElse(-1)))
      }
      case _ => None
    })

    ttrData.flatten

  }

}
