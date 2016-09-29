package com.grburst.libtt

import com.grburst.libtt.util.types._

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

// Use Options in fields where no information is possible
case class Player(
  id: Int,
  vRank: Rank,
  dRank: Rank,
  firstname: String,
  surname: String,
  club: String,
  clubId: Int,
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
