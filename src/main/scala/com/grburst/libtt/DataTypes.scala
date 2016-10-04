package com.grburst.libtt.types

import com.grburst.libtt.util.types._
import akka.http.scaladsl.model.headers.HttpCookie

case class User(
  id: Int,
  firstname: String,
  surname: String,
  var cookies: List[HttpCookie] = Nil,
  club: Option[String] = None,
  clubId: Option[Int] = None,
  organisation: Option[String] = None,
  league: Option[String] = None,
  leagueId: Option[Int] = None,
  qttr: Option[Int] = None,
  ttr: Option[Int] = None,
  vRank: Option[Int] = None,
  dRank: Option[Int] = None)

// object User {}

case class Club(
  id: Int,
  name: String,
  organisation: String,
  orgaId: Int)

case class MyMatch(
  firstname: String,
  surname: String,
  ttr: TTR,
  opponentId: Int,
  result: String,
  set1: String,
  set2: String,
  set3: String,
  set4: Option[String],
  set5: Option[String],
  set6: Option[String],
  set7: Option[String],
  ge: Float)

case class ProfileInfo(
  id: Int,
  firstname: String,
  surname: String,
  ttr: TTR,
  qttr: TTR,
  userId: Option[Int])

case class Player(
  id: Int,
  vRank: Rank,
  dRank: Rank,
  firstname: String,
  surname: String,
  club: String,
  clubId: Option[Int],
  ttr: TTR)

case class Event(
  sDate: String,
  lDate: String,
  name: String,
  id: Int,
  ak: Int,
  bilanz: String,
  gewinnerwartung: Float,
  typ: String,
  ttr: TTR,
  ttrDiff: TTR)
