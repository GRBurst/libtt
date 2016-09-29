package com.grburst.libtt

import spray.http.HttpCookie

case class User(
  id: Int,
  firstname: String,
  surname: String,
  club: Option[String],
  clubId: Option[Int],
  organisation: Option[String],
  league: Option[String],
  leagueId: Option[Int],
  qttr: Option[Int],
  ttr: Option[Int],
  vRank: Option[Int],
  dRank: Option[Int],
  cookies: List[HttpCookie])
