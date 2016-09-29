package com.grburst.libtt.parser

import com.grburst.libtt._
import com.grburst.libtt.util.types._
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.util.Try

import spray.http.Uri

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

case class MyTischtennisParser() {

  def parseUserMasterPage(doc: net.ruippeixotog.scalascraper.model.Document): Option[User] = {
    // further parse for .hideOwn[id] for id
    val user = (doc >> texts("div.userDatas > :not(.leftSite)")).toList
    if (user.length > 0) Some(User(0, user(0), user(1), Some(user(2)), None, Some(user(3)), Some(user(4)), None, user(5).toIntOption, None, None, None, Nil))
    else None
  }

  // TODO: implement player profile parser
  def parsePlayerProfile(doc: net.ruippeixotog.scalascraper.model.Document): Option[Player] = {
    None
  }

  // TODO: implement parser to find league / group id
  def findGroupId(doc: net.ruippeixotog.scalascraper.model.Document): Option[Int] = {
    None
  }

  def parseSearchPlayerList(doc: net.ruippeixotog.scalascraper.model.Document): List[Player] = {

    val ttrTable = doc >> element(".table-striped") >> element("tbody:nth-child(2)") >> elementList("tr")
    val ttrData: List[Option[Player]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(d, n, c, t, b, s) => {
        val pId: Array[String] = (n >> attr("data-tooltipdata")("a")).split(";")
        val name: Array[String] = pId(2).split(",")
        val uri = Uri(c >> attr("href")("a"));

        Some(Player(pId(0).toInt, None, d.text.toIntOption, name(1).trim, name(0).trim, c.text, uri.query.get("clubid").get.toInt, t.text.toIntOption))
      }
      case _ => None
    })

    ttrData.flatten

  }

  def parseSearchClubList(doc: net.ruippeixotog.scalascraper.model.Document): List[Club] = {

    val ttrTable = doc >> element("#ajaxclublist") >> elementList(".row")
    val ttrData: List[Option[Club]] = ttrTable.map(v => {
      val id = (v >> attr("data-club-nr")("a")).toInt
      val orga = (v >> attr("data-organisation")("a"))
      val orgaId = (v >> attr("data-club-id")("a")).toInt

      Try(Club(id, v.text, orga.toString, orgaId)).toOption
    })

    ttrData.flatten

  }

  def parseEvents(doc: net.ruippeixotog.scalascraper.model.Document): List[Event] = {

    val ttrTable = doc >> element("#tooltip-wrapper-ttr-table") >> element("tbody") >> elementList("tr")
    val ttrData: List[Option[Event]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(s, l, e, a, b, g, ty, tr, td) => {
        val eIdt: String = e >> attr("href")("a")
        val eId = eIdt.substring(eIdt.indexOf("(") + 1, eIdt.indexOf(",", eIdt.indexOf("(")))
        Some(Event(s.text, l.text, e.text, eId.toInt, a.text.toInt, b.text, g.text.replace(',', '.').toFloat, ty.text, tr.text.toIntOption, td.text.toIntOption))
      }
      case _ => None
    })

    ttrData.flatten

  }

  def parseEventDetail(doc: net.ruippeixotog.scalascraper.model.Document): List[MyMatch] = {

    def isTTSet(s: String) = if (s.length > 3) Some(s) else None

    val ttrTable = doc >> element(".table-striped") >> element("tbody") >> elementList("tr")
    val ttrData: List[Option[MyMatch]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(o, r, s1, s2, s3, s4, s5, s6, s7, g) => {
        val oIdt: Array[String] = (o >> attr("data-tooltipdata")("a")).split(";")
        val name: Array[String] = oIdt(2).split(",");
        val ottrt = o.text
        val ottr = ottrt.substring(ottrt.indexOf("(") + 1, ottrt.indexOf(")", ottrt.indexOf("(")))

        Some(MyMatch(name(1).trim, name(0).trim, ottr.toIntOption, oIdt(0).toInt, r.text, s1.text, s2.text, s3.text, isTTSet(s4.text), isTTSet(s5.text), isTTSet(s6.text), isTTSet(s7.text), g.text.replace(',', '.').toFloat))
      }
      case _ => None
    })

    ttrData.flatten

  }

  def parseRanking(doc: net.ruippeixotog.scalascraper.model.Document): List[Player] = {

    val ttrTable = doc >> element(".table-striped") >> elementList("tr")
    val ttrData: List[Option[Player]] = ttrTable.map(x => (x >> elementList("td")).toList match {
      case List(r, d, n, c, t, s) => {
        val pId: Array[String] = (n >> attr("data-tooltipdata")("a")).split(";")
        val name: Array[String] = pId(2).split(" ")
        val vr: Rank = r.text.split("/")(0).trim.toIntOption
        val uri = Uri(c >> attr("href")("a"));

        Some(Player(pId(0).toInt, vr, d.text.toIntOption, name(0).trim, name(1).trim, c.text, uri.query.get("clubid").get.toInt, t.text.toIntOption))
      }
      case _ => None
    })

    ttrData.flatten

  }

}
