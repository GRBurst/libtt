package libtt.test

import com.grburst.libtt._
import com.grburst.libtt.util.types._
import com.grburst.libtt.parser.MyTischtennisParser

import org.specs2._

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

class MyTischtennisParserSpec extends org.specs2.mutable.Specification {

  val parser = MyTischtennisParser()
  val browser = JsoupBrowser()

  "parsing the user's master page" >> {

    val user = parser.parseUserMasterPage(browser.parseFile("src/test/res/masterPage.htm")).get

    user.id mustEqual 0
    user.firstname mustEqual "Max"
    user.surname mustEqual "Mustermann"
    user.club.get mustEqual "TTV Musterverein"
    user.clubId mustEqual None
    user.organisation.get mustEqual "XTTV"
    user.league.get mustEqual "Herren-Spezialliga 1"
    user.leagueId mustEqual None
    user.qttr.get mustEqual 2526
    user.ttr mustEqual None
    user.vRank mustEqual None
    user.dRank mustEqual None
    user.cookies mustEqual Nil

    user mustEqual User(0, "Max", "Mustermann", Some("TTV Musterverein"), None, Some("XTTV"), Some("Herren-Spezialliga 1"), None, Some(2526), None, None, None, Nil)

  }

  "parseSearchPlayerList" >> todo

  "parse result of a club search" >> {

    val clubList = parser.parseSearchClubList(browser.parseFile("src/test/res/verein_search.htm"))

    clubList(0).id mustEqual 10100
    clubList(0).name mustEqual "TTV Musterclub 1"
    clubList(0).organisation mustEqual "XTTV"
    clubList(0).orgaId mustEqual 1010

    clubList(0) mustEqual Club(10100, "TTV Musterclub 1", "XTTV", 1010)
    clubList(1) mustEqual Club(100100, "TTV Musterclub 2", "YTTV", 1020)
    clubList(2) mustEqual Club(1000100, "TTV Musterclub 3", "XTTV", 1030)

  }

  "parse events page" >> {

    val eventsList = parser.parseEvents(browser.parseFile("src/test/res/events.htm"))

    eventsList(0).sDate mustEqual "25.05."
    eventsList(0).lDate mustEqual "25.05.2099"
    eventsList(0).name mustEqual "SL-Damen | TuS Musterverein II : TTV Musterverein"
    eventsList(0).id mustEqual 100900700
    eventsList(0).ak mustEqual 16
    eventsList(0).bilanz mustEqual "2:0"
    eventsList(0).gewinnerwartung mustEqual 1.608f
    eventsList(0).typ mustEqual "Mannschaft"
    eventsList(0).ttr.get mustEqual 2505
    eventsList(0).ttrDiff.get mustEqual 6

    eventsList(0) mustEqual Event("25.05.", "25.05.2099", "SL-Damen | TuS Musterverein II : TTV Musterverein", 100900700, 16, "2:0", 1.608f, "Mannschaft", Some(2505), Some(6))
    eventsList(1) mustEqual Event("04.05.", "04.05.2099", "SL-Damen | TuS Musterverein I : TTV Musterverein I", 100900800, 16, "1:1", 1.171f, "Mannschaft", Some(2515), Some(-2))
    eventsList(2) mustEqual Event("11.03.", "11.04.2099", "Crazy Spezial Turnier - Damen", 101202303, 16, "3:3", 3.001f, "Turnier", Some(2500), Some(0))
  }

  "parse event details" >> {

    val matchList = parser.parseEventDetail(browser.parseFile("src/test/res/eventDetails.htm"))

    matchList(0).firstname mustEqual "Max1"
    matchList(0).surname mustEqual "Mustermann1"
    matchList(0).ttr.get mustEqual 2000
    matchList(0).opponentId mustEqual 2000100
    matchList(0).result mustEqual "3:0"
    matchList(0).set1 mustEqual "11:5"
    matchList(0).set2 mustEqual "11:6"
    matchList(0).set3 mustEqual "11:7"
    matchList(0).set4 mustEqual None
    matchList(0).set5 mustEqual None
    matchList(0).set6 mustEqual None
    matchList(0).set7 mustEqual None
    matchList(0).ge mustEqual 0.511f

    matchList(0) mustEqual MyMatch("Max1", "Mustermann1", Some(2000), 2000100, "3:0", "11:5", "11:6", "11:7", None, None, None, None, 0.511f)

  }

  "ranking parser" >> {

    val playerList = parser.parseRanking(browser.parseFile("src/test/res/ranking.htm"))

    playerList(0).id mustEqual 100100
    playerList(0).vRank.get mustEqual 1
    playerList(0).dRank.get mustEqual 1000
    playerList(0).firstname mustEqual "Max"
    playerList(0).surname mustEqual "Mustermann"
    playerList(0).club mustEqual "Musterverein"
    playerList(0).clubId mustEqual 200100
    playerList(0).ttr.get mustEqual 2222
    playerList(0) mustEqual Player(100100, Some(1), Some(1000), "Max", "Mustermann", "Musterverein", 200100, Some(2222))
    playerList(1) mustEqual Player(100200, Some(2), Some(2000), "Maxi", "Musterfrau", "Musterverein", 200100, Some(2112))

  }

}
