package com.grburst.libtt

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }
import scala.collection.mutable

// import spray.caching.{LruCache, Cache}
import spray.client.pipelining._
import spray.http.{ FormData, HttpCookie, HttpRequest, HttpResponse }
import spray.http.Uri
import spray.http.HttpHeaders.{ Cookie, `Set-Cookie` }
import spray.httpx.encoding.Gzip

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import com.grburst.libtt.util.types._
import com.grburst.libtt.parser.MyTischtennisParser

// import scala.concurrent.Await
// import scala.concurrent.duration._

case class MyTischtennisBrowser() {

  // private var user = User(0, "Max", "Musterfrau", None, None, None, None, None, None, None, None, None, Nil)

  private val parser = MyTischtennisParser()
  private val cookieStorage: mutable.ArrayBuffer[HttpCookie] = mutable.ArrayBuffer()
  // val cache: Cache[HttpResponse] = LruCache()

  private val libttConf = ConfigFactory.parseString("""
    spray.can.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"
    spray.can.host-connector.max-redirects = 0""")

  implicit val system = ActorSystem("libtt-actors", libttConf)
  import system.dispatcher

  /**
   * Get user's data.
   * Depends on user data und current session
   */
  def getMyEvents(user: User, timeInterval: Option[String] = Some("last12Months")): Future[List[Event]] = {
    getPlayerEvents(Some(user.id), timeInterval)
  }

  def getMyFriendsRanking: Future[List[Player]] = {
    searchCustomRanking(Map("showmyfriends" -> "1"))
  }

  //https://www.mytischtennis.de/community/ajax/_rankingList?showgroupid=[id]
  def getMyLeagueRanking(user: User): Future[List[Player]] = {
    getLeagueRanking(user.leagueId.get)
  }

  def getMyClubRanking: Future[List[Player]] = {
    val playerProfileUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map("showmyclub" -> "1"))

    makeGetRequest(playerProfileUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)
  }

  def getMyPlayerProfile(user: User): Future[Option[Player]] = {
    getPlayerProfile(user.id, user.firstname, user.surname)
  }

  /**
   * Get data of a player. Therefore depends on given parameter of a player
   * and are more general
   */

  def getPlayerProfile(playerId: Int, playerFirstname: String, playerSurname: String): Future[Option[Player]] = {
    val playerProfileUrl = Uri("https://www.mytischtennis.de/community/ajax/_tooltippstuff")
      .withQuery(Map(
        "writetodb" -> "false",
        "otherUsersClickTTId" -> playerId.toString,
        "otherUsersUserId" -> "0",
        "nameOfOtherUser" -> (playerFirstname + " " + playerSurname)))

    makeGetRequest(playerProfileUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parsePlayerProfile(JsoupBrowser().parseString(hres.entity.asString))
      else None)
  }

  def getPlayerEvents(playerId: Option[Int] = None, timeInterval: Option[String] = Some("last12Months")): Future[List[Event]] = {
    val playerEventsUrl = Uri("https://www.mytischtennis.de/community/events")
    val request = timeInterval match {
      case None => playerId match {
        case None => Get(playerEventsUrl)
        case Some(player) => Get(playerEventsUrl.withQuery(Map("personId" -> player.toString)))
      }
      case Some(time) =>
        val data = FormData(Map("timeIntervalKeyWord" -> time))
        playerId match {
          case None => Post(playerEventsUrl, data)
          case Some(player) => Post(playerEventsUrl.withQuery(Map("personId" -> player.toString)), data)
        }
    }

    makeRequest(request) map (hres =>
      if (hres.entity.nonEmpty) parser.parseEvents(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)
  }

  // https://www.mytischtennis.de/community/ranking?showgroupid=275617
  def getLeagueRanking(leagueId: Int): Future[List[Player]] = {
    val leagueUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map(
        "groupid" -> leagueId.toString,
        "ttrQuartalorAktuell2" -> "aktuell"))

    makeGetRequest(leagueUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)
  }

  def getClubRanking(clubId: Int, clubName: String): Future[List[Player]] = {
    searchCustomRanking(Map("alleSpielberechtigen" -> "YES", "verband" -> "Alle",
      "verein" -> clubName, "vereinId" -> clubId.toString))
  }

  def getEventDetail(eventId: Int): Future[List[MyMatch]] = {
    val eventDetailUrl = Uri("https://www.mytischtennis.de/community/eventDetails")
      .withQuery(Map("fq" -> "false", "eventId" -> eventId.toString))

    makeGetRequest(eventDetailUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseEventDetail(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)

  }

  def searchClub(clubName: String): Future[List[Club]] = {
    val searchClubUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Map("trigger" -> "1", "d" -> (clubName)))

    makeGetRequest(searchClubUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)
  }

  def searchPlayer(firstName: String, lastName: String, clubId: Int, clubName: String, organisation: String): Future[List[Player]] = {
    searchCustomRanking(
      Map(
        "vorname" -> firstName, "nachname" -> lastName,
        "verein" -> (clubName + "," + organisation),
        "vereinId" -> clubId.toString),
      parser.parseSearchPlayerList)
  }

  def searchPlayer(firstName: String, lastName: String): Future[List[Player]] = {
    val searchPlayerUrl = Uri("https://www.mytischtennis.de/community/ranking")
      .withQuery(Map(
        "vorname" -> firstName,
        "nachname" -> lastName,
        "vereinIdPersonenSuche" -> "",
        "vereinPersonenSuche" -> "Verein+suchen"))

    makeGetRequest(searchPlayerUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseSearchPlayerList(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)
  }

  // TODO: Generate Formular for choices
  def searchCustomRanking(userChoice: Map[String, String], playerParser: net.ruippeixotog.scalascraper.model.Document => List[Player] = parser.parseRanking) = {
    val searchRankingUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map(
        "kontinent" -> "Europa",
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
        "showmyfriends" -> "0") ++ userChoice)

    makeGetRequest(searchRankingUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) playerParser(JsoupBrowser().parseString(hres.entity.asString))
      else Nil)
  }

  private def initMyMasterData: Future[Option[User]] = {
    val userUrl = "https://www.mytischtennis.de/community/userMasterPagePrint"
    makeGetRequest(userUrl).map(hres =>
      if (hres.entity.nonEmpty) parser.parseUserMasterPage(JsoupBrowser().parseString(hres.entity.asString))
      else None)
  }

  private def initMyClubData(user: Option[User]): Future[Option[Club]] = {
    val searchUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Map("trigger" -> "1", "d" -> (user.get.club.get)))

    makeGetRequest(searchUrl.toString).map(hres =>
      if (hres.entity.nonEmpty) Try(parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString)).head).toOption
      else None)
  }

  private def initMyLeagueData: Future[Option[Int]] = {
    val leagueUrl = "https://www.mytischtennis.de/community/group"

    makeGetRequest(leagueUrl).map(hres =>
      if (hres.entity.nonEmpty) parser.findGroupId(JsoupBrowser().parseString(hres.entity.asString))
      else None)
  }

  private def initMyPlayerData(user: Option[User], club: Option[Club]): Future[List[Player]] = {
    searchPlayer(user.get.firstname, user.get.surname, club.get.id, club.get.name, user.get.organisation.get)
  }

  private def caseClassToMap(cc: Option[AnyRef]) = {
    if (cc.isDefined) {
      (Map[String, Any]() /: cc.getClass.getDeclaredFields) { (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc))
      }
    } else
      Map[String, Any]()
  }

  private def userDataFusion(user: Option[User], club: Option[Club], playerList: List[Player]): User = {

    val params = caseClassToMap(user) ++ caseClassToMap(club) ++ caseClassToMap(Try(playerList.head).toOption)

    User(
      params.get("id").toString.toInt,
      params.get("firstname").toString,
      params.get("surname").toString,
      Try(params.get("club").toString).toOption,
      Try(params.get("clubId").toString.toInt).toOption,
      Try(params.get("organisation").toString).toOption,
      Try(params.get("league").toString).toOption,
      Try(params.get("leagueId").toString.toInt).toOption,
      Try(params.get("qttr").toString.toInt).toOption,
      Try(params.get("ttr").toString.toInt).toOption,
      Try(params.get("vRank").toString.toInt).toOption,
      Try(params.get("dRank").toString.toInt).toOption,
      Nil)
  }
  // case class User( id: Int, firstname: String, surname: String, club: Option[String], clubId: Option[Int], organisation: Option[String], league: Option[String], leagueId: Option[Int], qttr: Option[Int], ttr: Option[Int], vRank: Option[Int], dRank: Option[Int], cookies: List[HttpCookie])
  // case class Player( id: Int, vRank: Rank, dRank: Rank, name: String, club: String, clubId: Int, ttr: TTR)

  // Run this once after first login and after every transfer period
  // Ask user to confirm if data are correct
  private def initiateUser() = {
    // Step 1: parse users master page  => Get firstname, surname, club, organisation, league, qttr
    // Step 2: search my club           => Get clubId, clubName, organisation, orgaId
    // Step 3: search player (self)     => Get id, vRank, dRank, fullName, club, clubId, ttr
    // Step 4: ??                       => Get leagueId
    for {
      prematureUser <- initMyMasterData
      myClubData <- initMyClubData(prematureUser)
      myPlayerData <- initMyPlayerData(prematureUser, myClubData)
    } yield userDataFusion(prematureUser, myClubData, myPlayerData)

  }

  def login(username: String, pass: String) = {
    val l = doLogin(u, p)
    l onComplete {
      case Success(res) => {
        cookieStorage ++= extractCookies(res)
        println("Successfully logged in")
      }
      case Failure(e) => println(s"Error: $e")
    }
  }

  private def doLogin(userName: String, pass: String): Future[HttpResponse] = {
    val pipeline: HttpRequest => Future[HttpResponse] = (sendReceive)
    val data = FormData(Map("userNameB" -> userName, "userPassWordB" -> pass))
    val request = Post("https://www.mytischtennis.de/community/login", data)

    pipeline(request)
  }

  def terminate() = system.terminate()

  private def addCookies(): HttpRequest => HttpRequest = {
    req: HttpRequest =>
      {
        if (cookieStorage.isEmpty) req
        else addHeader(Cookie(cookieStorage))(req)
      }
  }

  private def extractCookies(resp: HttpResponse): List[HttpCookie] = resp.headers.collect { case `Set-Cookie`(cookie) => cookie }

  private def makeRequest(request: HttpRequest): Future[HttpResponse] = {
    val pipeline: HttpRequest => Future[HttpResponse] = (addCookies
      ~> addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      ~> addHeader("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
      ~> addHeader("Accept-Encoding", "gzip")
      ~> encode(Gzip)
      ~> sendReceive
      ~> decode(Gzip))

    pipeline(request)
  }

  private def makeGetRequest(url: String): Future[HttpResponse] = makeRequest(Get(url))
  private def makePostRequest(url: String, form: FormData): Future[HttpResponse] = makeRequest(Post(url, form))

  // def makeCachedRequest(url: String) = cache(url) {
  //   makeGetRequest(url)
  // }

}
