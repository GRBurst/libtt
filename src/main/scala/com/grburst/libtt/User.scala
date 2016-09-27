package com.grburst.libtt

import spray.http.HttpCookie

case class User(id: Int,
  firstname: String,
  surname:String,
  club: String,
  clubId: Int,
  organisation: String,
  league: String,
  leagueId: Option[Int],
  qttr: Option[Int],
  ttr: Option[Int],
  cookies: List[HttpCookie])
