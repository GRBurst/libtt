package com.grburst.libtt.parser

import com.grburst.libtt.Club
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import spray.http.Uri

import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._

case class ClubSearchParser(doc: net.ruippeixotog.scalascraper.model.Document) {

  def get: List[Club] = {

    val ttrTable = doc >> element("#ajaxclublist") >> elementList(".row")
    val ttrData: List[Option[Club]] = ttrTable.map(v => {
        val id = (v >> attr("data-club-nr")("a")).toInt
        val orga = (v >> attr("data-organisation")("a"))
        val orgaId = (v >> attr("data-club-id")("a")).toInt

        Try(Club(id, v.text, orga.toString, orgaId)).toOption
    })

    ttrData.flatten

  }

}
