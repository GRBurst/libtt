package com.grburst.libtt

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import scala.collection.mutable

import spray.caching.{LruCache, Cache}
import spray.client.pipelining._
import spray.http._
import spray.http.Uri
import spray.http.HttpHeaders._
import spray.httpx.encoding.{Gzip, Deflate}

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import com.grburst.libtt.util.types._
import com.grburst.libtt.parser.MyTischtennisParser

import scala.concurrent.Await
import scala.concurrent.duration._

case class MyTischtennisBrowser() {

  private var user:Option[User] = None

  private val parser = MyTischtennisParser()
  private val cookieStorage: mutable.ArrayBuffer[HttpCookie] = mutable.ArrayBuffer()
  // val cache: Cache[HttpResponse] = LruCache()

  // spray.can.client.user-agent-header = "Mozilla/5.0 (X11; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0"
  // spray.can.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"
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

  def getMyLeague: Future[List[Player]] = {
    //TEST-URL
    val myLeagueUrl = "http://www.mytischtennis.de/community/ajax/_rankingList?groupid=275617&ttrQuartalorAktuell2=aktuell&goAssistentG=Anzeigen"

    makeRequest(myLeagueUrl) map(hres =>
      if(hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  def getMyClub: Future[List[Player]] = {
    //TEST-URL
    val myClubUrl = "https://www.mytischtennis.de/community/ajax/_rankingList?kontinent=Europa&land=DE&deutschePlusGleichgest=no&alleSpielberechtigen=YES&verband=Alle&bezirk=&kreis=&regionPattern123=&regionPattern4=&regionPattern5=&geschlecht=&geburtsJahrVon=&geburtsJahrBis=&ttrVon=&ttrBis=&ttrQuartalorAktuell=aktuell&anzahlErgebnisse=100&vorname=&nachname=&verein=VfL%20SuS%20Borussia%20Brand&vereinId=157008%2CWTTV&vereinPersonenSuche=&vereinIdPersonenSuche=&ligen=&groupId=&showGroupId=&deutschePlusGleichgest2=no&ttrQuartalorAktuell2=aktuell&showmyfriends=0"

    makeRequest(myClubUrl) map(hres =>
      if(hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  def getMyEvents: Future[List[Event]] = {
    val eventsUrl = "https://www.mytischtennis.de/community/events"
    makeRequest(eventsUrl) map(hres =>
      if(hres.entity.nonEmpty) parser.parseEvents(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  def getEventDetail: Future[List[MyMatch]] = {
    //TEST-URL
    val eventDetailUrl = "https://www.mytischtennis.de/community/eventDetails?fq=false&eventId=186649490"
    makeRequest(eventDetailUrl) map(hres =>
      if(hres.entity.nonEmpty) parser.parseEventDetail(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )

  }

  def searchClub(clubName: String): Future[List[Club]] = {
    val searchClubUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox").withQuery(Map("trigger" -> "1", "d"-> (clubName)))
    makeRequest(searchClubUrl.toString) map(hres =>
      if(hres.entity.nonEmpty) parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString))
      else Nil
    )
  }

  // def searchRanking: Future[List[Club]] = {
  //   val searchRankingUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList").withQuery(Map("kontinent" -> "Europa",
  //     "land" -> "DE",
  //     "deutschePlusGleichgest" -> "no",
  //     "alleSpielberechtigen" -> "",
  //     "verband" -> "",
  //     "bezirk" -> "",
  //     "kreis" -> "",
  //     "regionPattern123" -> "",
  //     "regionPattern4" -> "",
  //     "regionPattern5" -> "",
  //     "geschlecht" -> "",
  //     "geburtsJahrVon" -> "",
  //     "geburtsJahrBis" -> "",
  //     "ttrVon" -> "",
  //     "ttrBis" -> "",
  //     "ttrQuartalorAktuell" -> "aktuell",
  //     "anzahlErgebnisse" -> "100",
  //     "vorname" -> user.firstname,
  //     "nachname" -> user.surname,
  //     "verein" -> "",
  //     "vereinId" -> "",
  //     "vereinPersonenSuche" -> user.club,
  //     "vereinIdPersonenSuche" -> user.clubId,//%2CWTTV
  //     "ligen" -> "",
  //     "groupId" -> "",
  //     "showGroupId" -> "",
  //     "deutschePlusGleichgest2" -> "no",
  //     "ttrQuartalorAktuell2" -> "aktuell",
  //     "showmyfriends" -> "0",
  // }

  def tests() = {

    println("Get my events")
    getMyEvents onSuccess {
      case l =>
        println(s"Events Success: ${l.take(10).toString}")

        println("Get my club")
        getMyClub onSuccess {
          case l =>
            println(s"Club Success: ${l.take(10).toString}")

            println("Get my league")
            getMyLeague onSuccess {
              case l =>
                println(s"League Success: ${l.take(10).toString}")

                println("Get event Detail")
                getEventDetail onSuccess {
                case l =>
                  println(s"EventDetail Success: ${l.toString}")

                }

            }

        }

    }

    // shutdown()

  }

  def shutdown() = {
    system.shutdown()
  }

  def debugPrintCookies() = println(s"cookies stored = ${cookieStorage.toString}")
  // def searchClub(clubName: String): List[Club] = {}
  // def searchPlayer(firstName: String, lastName): Club = {}

  // def getClubById(clubId: Int, clubName: String): Club = {}
  // def getEventById(eventId: Int): Event = {}
  // def getMatchById(matchId: Int): MyMatch = {}
  // def getPlayerById(playerId: Int): Player = {}

  private def initiateUser() = {
    val userUrl = "http://www.mytischtennis.de/community/userMasterPagePrint"
    val tUser:Future[Option[User]] = makeRequest(userUrl).map(hres =>
        if(hres.entity.nonEmpty) parser.parseUserMasterPage(JsoupBrowser().parseString(hres.entity.asString))
        else None)

    val fClub:Future[Option[Club]] = tUser.flatMap(u => {
      val searchUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox").withQuery(Map("trigger" -> "1", "d"-> (u.get.club)))
      makeRequest(searchUrl.toString).map(hres =>
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

  private def makeRequest(url: String):Future[HttpResponse] = {
    val pipeline: HttpRequest => Future[HttpResponse] = (addCookies
      ~> addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      ~> addHeader("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
      ~> addHeader("Accept-Encoding", "gzip")
      ~> encode(Gzip)
      ~> sendReceive
      ~> decode(Gzip))

    pipeline(Get(url))

  }

  // def extractRequest[T]: List[T](hres: HttpResponse) = {

  //   if(hres.entity.nonEmpty){
  //     val browser = JsoupBrowser()
  //     parse(browser.parseString(hres.entity.asString))
  //   }

  // }

  // def makeCachedRequest(url: String) = cache(url) {
  //   makeRequest(url)
  // }

}

// Dalvik/2.1.0 (Linux; U; Android 6.0.1; A0001 Build/MMB29V)
// addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0")
