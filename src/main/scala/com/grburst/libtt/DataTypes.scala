package com.grburst.libtt

case class Match(
  opponent: String,
  oTTR: Int,
  oId: Int,
  result: String,
  set1: String,
  set2: String,
  set3: String,
  set4: String,
  set5: String,
  set6: String,
  set7: String,
  ge: Float)

case class Player(
  playerId: Int,
  rank: String,
  dRank: Int,
  name: String,
  club: String,
  clubId: Int,
  ttr: Int)

case class Event(sDate:String,
  lDate:String,
  name:String,
  id:Int,
  ak:String,
  bilanz:String,
  gewinnerwartung:String,
  typ:String,
  ttr:Int,
  ttrDiff:Int)

