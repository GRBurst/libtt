package com.grburst.libtt.parser

import com.grburst.libtt._
import com.grburst.libtt.types._
import com.grburst.libtt.util.types._
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.util.Try

import akka.http.scaladsl.model.Uri

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

// TODO: Wrap extractions in Options since scraper throws execption if element is not found
case class MyTischtennisParser() {

  def findGroupId(doc: HttpDoc): Option[Int] = {
    Try(Uri(doc >> element("div.panel-body") >> attr("href")("a")).query().get("groupId").get.toInt).toOption
  }

  def isLoginPage(doc: HttpDoc): Option[Boolean] = {
    Try((doc >> text("title")).contains("Login")).toOption
  }

  def parseUserMasterPage(doc: HttpDoc): Option[User] = {
    // further parse for .hideOwn[id] for id
    Try({
      val user = (doc >> texts("div.userDatas > :not(.leftSite)")).toList
      User(0, user(0), user(1), Nil, Some(user(2)), None, Some(user(3)), Some(user(4)), None, user(5).toIntOption)
    }).toOption
  }

  def parsePlayerProfile(doc: HttpDoc): Option[ProfileInfo] = {
    Try({
      val p = "(\\d+)".r
      val name = (doc >> text("h2")).split(" ")
      val id = Uri(doc >> attr("href")("a.ttrIcon")).query().get("personId")
      val userId = Try(Uri(doc >> attr("href")("a.infoIcon")).query().get("userid").get.toInt).toOption
      val ttr = p.findAllIn(doc >> text(".label-primary")).toArray

      ProfileInfo(id.get.toInt, name(0).trim, name(1).trim, ttr(0).toIntOption, ttr(1).toIntOption, userId)
    }).toOption
  }

  def parseSearchPlayerList(doc: HttpDoc): Option[List[Player]] = {

    Try({
      val ttrTable = doc >> element(".table-striped") >> elementList("tr")
      // val ttrData: List[Option[Player]] = ttrTable match{case Some(x) =>
      val ttrData: List[Option[Player]] = ttrTable.drop(1).map(x => (x >> elementList("td")).toList match {
        case List(d, n, c, t, b, s) => Try({
          val pId: Array[String] = (n >> attr("data-tooltipdata")("a")).split(";")
          val name: Array[String] = pId(2).split(" ")
          val clubId = Try(Uri(c >> attr("href")("a")).query().get("clubid").get.toInt).toOption;

          Player(pId(0).toInt, None, d.text.toIntOption, name(1).trim, name(0).trim, c.text, clubId, t.text.toIntOption)
        }).toOption
        case _ => None
      })

      ttrData.flatten
    }).toOption

  }

  def parseSearchClubList(doc: HttpDoc): Option[List[Club]] = {

    Try({
      val ttrTable = doc >> element("#ajaxclublist") >> elementList(".row")
      val ttrData: List[Option[Club]] = ttrTable.map(v => Try({
        val id = (v >> attr("data-club-nr")("a")).toInt
        val orga = (v >> attr("data-organisation")("a"))
        val orgaId = (v >> attr("data-club-id")("a")).toInt

        Club(id, v.text, orga.toString, orgaId)
      }).toOption)

      ttrData.flatten

    }).toOption
  }

  def parseEvents(doc: HttpDoc): Option[List[Event]] = {

    Try({
      val ttrTable = doc >> element("#tooltip-wrapper-ttr-table") >> element("tbody") >> elementList("tr")
      val ttrData: List[Option[Event]] = ttrTable.map(x => (x >> elementList("td")).toList match {
        case List(s, l, e, a, b, g, ty, tr, td) => Try({
          val eIdt: String = e >> attr("href")("a")
          val eId = eIdt.substring(eIdt.indexOf("(") + 1, eIdt.indexOf(",", eIdt.indexOf("(")))
          Event(s.text, l.text, e.text, eId.toInt, a.text.toInt, b.text, g.text.replace(',', '.').toFloat, ty.text, tr.text.toIntOption, td.text.toIntOption)
        }).toOption
        case _ => None
      })
      ttrData.flatten

    }).toOption
  }

  def parseEventDetail(doc: HttpDoc): Option[List[MyMatch]] = {

    def isTTSet(s: String) = if (s.length > 3) Some(s) else None

    Try({
      val ttrTable = doc >> element(".table-striped") >> element("tbody") >> elementList("tr")
      val ttrData: List[Option[MyMatch]] = ttrTable.map(x => (x >> elementList("td")).toList match {
        case List(o, r, s1, s2, s3, s4, s5, s6, s7, g) => Try({
          val oIdt: Array[String] = (o >> attr("data-tooltipdata")("a")).split(";")
          val name: Array[String] = oIdt(2).split(",");
          val ottrt = o.text
          val ottr = ottrt.substring(ottrt.indexOf("(") + 1, ottrt.indexOf(")", ottrt.indexOf("(")))

          MyMatch(name(1).trim, name(0).trim, ottr.toIntOption, oIdt(0).toInt, r.text, s1.text, s2.text, s3.text, isTTSet(s4.text), isTTSet(s5.text), isTTSet(s6.text), isTTSet(s7.text), g.text.replace(',', '.').toFloat)
        }).toOption
        case _ => None
      })
      ttrData.flatten

    }).toOption
  }

  def parseRanking(doc: HttpDoc): Option[List[Player]] = {

    Try({
      val ttrTable = doc >> element(".table-striped") >> elementList("tr")
      val ttrData: List[Option[Player]] = ttrTable.map(x => (x >> elementList("td")).toList match {
        case List(r, d, n, c, t, s) => Try({
          val pId: Array[String] = (n >> attr("data-tooltipdata")("a")).split(";")
          val name: Array[String] = pId(2).split(" ")
          val vr: Rank = r.text.split("/")(0).trim.toIntOption
          val clubId = Try(Uri(c >> attr("href")("a")).query().get("clubid").get.toInt).toOption;

          Player(pId(0).toInt, vr, d.text.toIntOption, name(0).trim, name(1).trim, c.text, clubId, t.text.toIntOption)
        }).toOption
        case _ => None
      })
      ttrData.flatten

    }).toOption
  }

}
