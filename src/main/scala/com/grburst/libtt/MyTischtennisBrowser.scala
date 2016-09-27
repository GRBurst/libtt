package com.grburst.libtt

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import scala.collection.mutable

// import spray.caching.{LruCache, Cache}
import spray.client.pipelining._
import spray.http.{FormData, HttpCookie, HttpRequest, HttpResponse}
import spray.http.Uri
import spray.http.HttpHeaders.{Cookie, `Set-Cookie`}
import spray.httpx.encoding.Gzip

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import com.grburst.libtt.util.types._
import com.grburst.libtt.parser.MyTischtennisParser

// import scala.concurrent.Await
// import scala.concurrent.duration._

case class MyTischtennisBrowser() {

  private var user:Option[User] = None

  private val parser = MyTischtennisParser()
  private val cookieStorage: mutable.ArrayBuffer[HttpCookie] = mutable.ArrayBuffer()
  // val cache: Cache[HttpResponse] = LruCache()

  private val libttConf = ConfigFactory.parseString("""
    spray.can.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"
    spray.can.host-connector.max-redirects = 0"""
  )

  implicit val system = ActorSystem("libtt-actors", libttConf)
  import system.dispatcher

  def login(username:String, pass:String) = {

    val l = doLogin(u, p)
    l onComplete {
      case Success(res) => {
        cookieStorage ++= extractCookies(res)
        println("Successfully logged in")
      }
      case Failure(e) => println(s"Error: $e")
    }

  }

  def getMyEvents(timeInterval: Option[String] = Some("last12Months")): Future[List[Event]] = {
    getPlayerEvents(Some(user.get.id.toString), timeInterval)
  }

  def getMyFriendsRanking: Future[List[Player]] = {
    searchCustomRanking(Map("showmyfriends" -> "1"))
  }

  def getMyLeagueRanking: Future[List[Player]] = {
    getLeagueRanking(user.get.leagueId.toString)
  }

  def getMyClubRanking: Future[List[Player]] = {
    getClubRanking(user.get.clubId.toString, user.get.club)
  }

  // TODO: implement player profile parser
//   def getPlayerProfileById(playerId: String, playerName: String): Future[Player] = {
//     val playerProfileUrl = Uri("https://www.mytischtennis.de/community/ajax/_tooltippstuff")
//       .withQuery(Map(
//         "writetodb" -> "false",
//         "otherUsersClickTTId" -> playerId,
//         "otherUsersUserId" -> "0",
//         "nameOfOtherUser" -> playerName))

//       makeGetRequest(playerProfileUrl.toString) map(hres =>
//       if(hres.entity.nonEmpty) parser.parsePlayerProfile(JsoupBrowser().parseString(hres.entity.asString))
//       else Nil
//     )
//   }

  def getPlayerEvents(playerId: Option[String] = None, timeInterval: Option[String] = None): Future[List[Event]] = {
    val playerEventsUrl = Uri("https://www.mytischtennis.de/community/events")
    val request = timeInterval match {
      case None => playerId match {
        case None => Get(playerEventsUrl)
        case Some(player) => Get(playerEventsUrl.withQuery(Map("personId" -> player)))
      }
      case Some(time) =>
        val data = FormData(Map("timeIntervalKeyWord" -> time))
        playerId match {
          case None => Post(playerEventsUrl, data)
          case Some(player) => Post(playerEventsUrl.withQuery(Map("personId" -> player)), data)
      }
    }

    makeRequest(request) map(hres =>
      if(hres.entity.nonEmpty) parser.parseEvents(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )

  }

  def getLeagueRanking(leagueId: String): Future[List[Player]] = {
    val leagueUrl = Uri("http://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map(
        "groupid" -> leagueId,
        "ttrQuartalorAktuell2" -> "aktuell",
        "goAssistentG" -> "Anzeigen"))

    makeGetRequest(leagueUrl.toString) map(hres =>
      if(hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  def getClubRanking(clubId: String, clubName: String): Future[List[Player]] = {
    searchCustomRanking(Map("alleSpielberechtigen" -> "YES", "verband" -> "Alle",
      "verein" -> clubName, "vereinId" -> clubId))
  }

  def getEventDetail(eventId: String): Future[List[MyMatch]] = {
    val eventDetailUrl = Uri("https://www.mytischtennis.de/community/eventDetails")
      .withQuery(Map("fq" -> "false", "eventId" -> eventId))

    makeGetRequest(eventDetailUrl.toString) map(hres =>
      if(hres.entity.nonEmpty) parser.parseEventDetail(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )

  }

  def searchClub(clubName: String): Future[List[Club]] = {
    val searchClubUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Map("trigger" -> "1", "d"-> (clubName)))

    makeGetRequest(searchClubUrl.toString) map(hres =>
      if(hres.entity.nonEmpty) parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  def searchPlayer(firstName: String, lastName: String, clubId: String, clubName: String, organisation: String): Future[List[Player]] = {
    searchCustomRanking(Map(
      "vorname" -> firstName, "nachname" -> lastName,
      "verein" -> (clubName + "," + organisation),
      "vereinId" -> clubId))
  }

  def searchPlayer(firstName: String, lastName: String): Future[List[Player]] = {
    val searchPlayerUrl = Uri("https://www.mytischtennis.de/community/ranking")
      .withQuery(Map(
        "panel" -> "2",
        "vorname" -> firstName,
        "nachname" -> lastName,
        "vereinIdPersonenSuche" -> "",
        "vereinPersonenSuche" -> "Verein+suchen",
        "goAssistentP" -> "Anzeigen"))

    makeGetRequest(searchPlayerUrl.toString) map(hres =>
      if(hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }
  // def searchPlayer(firstName: String, lastName): Club = {}

  def searchCustomRanking(userChoice: Map[String, String]): Future[List[Player]] = {
    val searchRankingUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map("kontinent" -> "Europa",
      "land" -> "DE",
      "deutschePlusGleichgest" -> "no",
      "alleSpielberechtigen" -> "",
      "verband" -> "",
      "bezirk" -> "",
      "kreis" -> "",
      "regionPattern123" -> "",
      "regionPattern4" -> "",
      "regionPattern5" -> "",
      "geschlecht" -> "",
      "geburtsJahrVon" -> "",
      "geburtsJahrBis" -> "",
      "ttrVon" -> "",
      "ttrBis" -> "",
      "ttrQuartalorAktuell" -> "aktuell",
      "anzahlErgebnisse" -> "100",
      "vorname" -> "",
      "nachname" -> "",
      "verein" -> "",
      "vereinId" -> "",
      "vereinPersonenSuche" -> "",
      "vereinIdPersonenSuche" -> "",
      "ligen" -> "",
      "groupId" -> "",
      "showGroupId" -> "",
      "deutschePlusGleichgest2" -> "no",
      "ttrQuartalorAktuell2" -> "aktuell",
      "showmyfriends" -> "0"
      ) ++ userChoice
    )

    makeGetRequest(searchRankingUrl.toString) map(hres =>
      if(hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  // def tests() = {

  //   println("Get my events")
  //   getMyEvents() onSuccess {
  //     case l =>
  //       println(s"Events Success: ${l.take(10).toString}")

  //       println("Get my club")
  //       getMyClubRanking onSuccess {
  //         case l =>
  //           println(s"Club Success: ${l.take(10).toString}")

  //           println("Get my league")
  //           getMyLeagueRanking onSuccess {
  //             case l =>
  //               println(s"League Success: ${l.take(10).toString}")

  //               println("Get event Detail")
  //               getEventDetail onSuccess {
  //               case l =>
  //                 println(s"EventDetail Success: ${l.toString}")

  //               }

  //           }

  //       }

  //   }

  // }

  def terminate() = {
    system.terminate()
  }

  def debugPrintCookies() = println(s"cookies stored = ${cookieStorage.toString}")

  private def initiateUser() = {
    val userUrl = "http://www.mytischtennis.de/community/userMasterPagePrint"
    val tUser:Future[Option[User]] = makeGetRequest(userUrl).map(hres =>
        if(hres.entity.nonEmpty) parser.parseUserMasterPage(JsoupBrowser().parseString(hres.entity.asString))
        else None)

    val fClub:Future[Option[Club]] = tUser.flatMap(u => {
      val searchUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox").withQuery(Map("trigger" -> "1", "d"-> (u.get.club)))
      makeGetRequest(searchUrl.toString).map(hres =>
          if(hres.entity.nonEmpty) Some(parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString)).head)
          else None)
    })

    // val fClub.flatMap(hres => if(hres.entity.nonEmpty) {

    // }


  }

  private def addCookies(): HttpRequest => HttpRequest = {
    req: HttpRequest => {
      if (cookieStorage.isEmpty) req
      else addHeader(Cookie(cookieStorage))(req)
    }
  }

  private def extractCookies(resp: HttpResponse): List[HttpCookie] = resp.headers.collect { case `Set-Cookie`(cookie) => cookie }

  private def doLogin(userName: String, pass: String): Future[HttpResponse] = {

    val pipeline: HttpRequest => Future[HttpResponse] = (sendReceive)

    val data = FormData(Map("userNameB" -> userName, "userPassWordB" -> pass))
    val request = Post("http://www.mytischtennis.de/community/login", data)

    pipeline(request)

  }

  private def makeRequest(request: HttpRequest):Future[HttpResponse] = {
    val pipeline: HttpRequest => Future[HttpResponse] = (addCookies
      ~> addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      ~> addHeader("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
      ~> addHeader("Accept-Encoding", "gzip")
      ~> encode(Gzip)
      ~> sendReceive
      ~> decode(Gzip))

    pipeline(request)
  }

  private def makeGetRequest(url: String):Future[HttpResponse] = {
    makeRequest(Get(url))
  }

  private def makePostRequest(url: String, form: FormData):Future[HttpResponse] = {
    makeRequest(Post(url, form))
  }

  // def makeCachedRequest(url: String) = cache(url) {
  //   makeGetRequest(url)
  // }

}
