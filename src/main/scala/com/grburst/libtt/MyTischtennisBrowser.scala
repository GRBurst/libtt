package com.grburst.libtt

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }
import scala.concurrent.Await
import scala.concurrent.duration._

// import scala.collection.mutable

// import spray.caching.{LruCache, Cache}
import spray.client.pipelining._
import spray.http.{ FormData, HttpCookie, HttpRequest, HttpResponse }
import spray.http.Uri
import spray.http.HttpHeaders.{ Cookie, `Set-Cookie` }
import spray.httpx.encoding.Gzip

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import com.grburst.libtt.types._
import com.grburst.libtt.parser.MyTischtennisParser

// case class MyTischtennisBrowser(credentialStorage: (u: String, p: String)) { <- Use something like this to supply credentials
case class MyTischtennisBrowser() {

  private val parser = MyTischtennisParser()
  // val cache: Cache[HttpResponse] = LruCache()

  // spray.can.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"
  private val libttConf = ConfigFactory.parseString("""
    spray.can.client.user-agent-header = "Mozilla/5.0 (X11; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0"
    spray.can.host-connector.max-redirects = 0""")

  implicit val system = ActorSystem("libtt-actors", libttConf)
  import system.dispatcher

  /**
   * Get user's data.
   * Depends on user data und current session
   */
  def getMyEvents(timeInterval: Option[String] = Some("last12Months"))(implicit user: User): Future[List[Event]] = {
    getPlayerEvents(Some(user.id), timeInterval)
  }

  def getMyFriendsRanking(implicit user: User): Future[List[Player]] = {
    searchCustomRanking(Map("showmyfriends" -> "1"))
  }

  //https://www.mytischtennis.de/community/ajax/_rankingList?showgroupid=[id]
  def getMyLeagueRanking(implicit user: User): Future[List[Player]] = {
    getLeagueRanking(user.leagueId.get)
  }

  def getMyClubRanking(implicit user: User): Future[List[Player]] = {
    val clubRankingUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map("showmyclub" -> "1"))

    makeGetRequest(clubRankingUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString)).getOrElse(Nil)
      else Nil)
  }

  def getMyPlayerProfile(implicit user: User): Future[Option[ProfileInfo]] = {
    getPlayerProfile(user.id, user.firstname, user.surname)
  }

  /**
   * Get data of a player. Therefore depends on given parameter of a player
   * and are more general
   */

  def getPlayerProfile(playerId: Int, playerFirstname: String, playerSurname: String)(implicit user: User): Future[Option[ProfileInfo]] = {
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

  def getPlayerEvents(playerId: Option[Int] = None, timeInterval: Option[String] = Some("last12Months"))(implicit user: User): Future[List[Event]] = {
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
      if (hres.entity.nonEmpty) parser.parseEvents(JsoupBrowser().parseString(hres.entity.asString)).getOrElse(Nil)
      else Nil)
  }

  // https://www.mytischtennis.de/community/ranking?showgroupid=275617
  def getLeagueRanking(leagueId: Int)(implicit user: User): Future[List[Player]] = {
    val leagueUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Map(
        "groupid" -> leagueId.toString,
        "ttrQuartalorAktuell2" -> "aktuell"))

    makeGetRequest(leagueUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseRanking(JsoupBrowser().parseString(hres.entity.asString)).getOrElse(Nil)
      else Nil)
  }

  def getClubRanking(club: Club)(implicit user: User): Future[List[Player]] = {
    searchCustomRanking(Map("alleSpielberechtigen" -> "YES", "verband" -> "Alle",
      "verein" -> club.name,
      "vereinId" -> (club.id.toString + "," + club.organisation)))
  }

  def getEventDetail(eventId: Int)(implicit user: User): Future[List[MyMatch]] = {
    val eventDetailUrl = Uri("https://www.mytischtennis.de/community/eventDetails")
      .withQuery(Map("fq" -> "false", "eventId" -> eventId.toString))

    makeGetRequest(eventDetailUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseEventDetail(JsoupBrowser().parseString(hres.entity.asString)).getOrElse(Nil)
      else Nil)

  }

  def searchClub(clubName: String)(implicit user: User): Future[List[Club]] = {
    val searchClubUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Map("trigger" -> "1", "d" -> (clubName)))

    makeGetRequest(searchClubUrl.toString) map (hres =>
      if (hres.entity.nonEmpty) parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString)).getOrElse(Nil)
      else Nil)
  }

  // firstName && lastname must contain 3+ chars each
  def searchPlayer(firstName: String, lastName: String, clubId: Int, clubName: String, organisation: String)(implicit user: User): Future[List[Player]] = {
    searchCustomRanking(
      Map(
        "vorname" -> firstName, "nachname" -> lastName,
        "verein" -> clubName,
        "vereinId" -> (clubId.toString + "," + organisation)),
      parser.parseSearchPlayerList)
  }

  // firstName && lastname must contain 3+ chars each
  def searchPlayer(firstName: String, lastName: String)(implicit user: User): Future[List[Player]] = {
    searchCustomRanking(
      Map(
        "vorname" -> firstName, "nachname" -> lastName,
        "vereinIdPersonenSuche" -> "",
        "vereinPersonenSuche" -> "Verein+suchen"),
      parser.parseSearchPlayerList)
  }

  // TODO: Generate Formular for choices
  def searchCustomRanking(userChoice: Map[String, String], playerParser: net.ruippeixotog.scalascraper.model.Document => Option[List[Player]] = parser.parseRanking)(implicit user: User): Future[List[Player]] = {
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
      if (hres.entity.nonEmpty) playerParser(JsoupBrowser().parseString(hres.entity.asString)).getOrElse(Nil)
      else Nil)
  }

  private def initMyMasterData(implicit user: User): Future[Option[User]] = {
    val userUrl = "https://www.mytischtennis.de/community/userMasterPagePrint"
    makeGetRequest(userUrl).map(hres =>
      if (hres.entity.nonEmpty) {
        val tmpUser = parser.parseUserMasterPage(JsoupBrowser().parseString(hres.entity.asString))
        if (tmpUser.isDefined) tmpUser.get.cookies ++= user.cookies
        tmpUser
      } else None)
  }

  private def initMyClubData(implicit user: User): Future[Option[Club]] = {
    val searchUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Map("trigger" -> "1", "d" -> (user.club.get)))

    makeGetRequest(searchUrl.toString).map(hres =>
      if (hres.entity.nonEmpty)
        Try(parser.parseSearchClubList(JsoupBrowser().parseString(hres.entity.asString)).get.head).toOption
      else None)
  }

  private def initMyLeagueData(implicit user: User): Future[Option[Int]] = {
    val leagueUrl = "https://www.mytischtennis.de/community/group"

    makeGetRequest(leagueUrl).map(hres =>
      if (hres.entity.nonEmpty) parser.findGroupId(JsoupBrowser().parseString(hres.entity.asString))
      else None)
  }

  private def initMyPlayerData(club: Option[Club])(implicit user: User): Future[Option[Player]] = {
    (searchPlayer(user.firstname, user.surname, club.get.id, club.get.name, user.organisation.get)).map(l =>
      Try(l.head).toOption)
  }

  private def caseClassToMap(cc: Option[AnyRef]) = {
    if (cc.isDefined) {
      (Map[String, Any]() /: cc.get.getClass.getDeclaredFields) { (a, f) =>
        f.setAccessible(true)
        a + (f.getName -> f.get(cc.get))
      }
    } else
      Map[String, Any]()
  }

  private def userDataFusion(user: Option[User], club: Option[Club], player: Option[Player], leagueId: Option[Int]): User = {
    // val params = caseClassToMap(user) ++
    //   caseClassToMap(club) ++
    //   caseClassToMap(Try(playerList.head).toOption)

    User(
      Try(player.get.id).getOrElse(0),
      Try(user.get.firstname).getOrElse(""),
      Try(user.get.surname).getOrElse(""),
      Try(user.get.cookies).getOrElse(Map()),
      Try(club.get.name).toOption,
      Try(club.get.id).toOption,
      Try(user.get.organisation).getOrElse(None),
      Try(user.get.league).getOrElse(None),
      leagueId,
      Try(user.get.qttr).getOrElse(None),
      Try(player.get.ttr).getOrElse(None),
      Try(player.get.vRank).getOrElse(None),
      Try(player.get.dRank).getOrElse(None))
  }

  // Run this once after first login and after every transfer period
  // Ask user to confirm if data are correct
  def initiateUser(user: User): Future[User] = {
    // Step 1: parse users master page  => Get firstname, surname, club, organisation, league, qttr
    // Step 2: search my club           => Get clubId, clubName, organisation, orgaId
    // Step 3: search player (self)     => Get id, vRank, dRank, fullName, club, clubId, ttr
    // Step 4: search group info page   => Get leagueId
    for {
      prematureUser <- initMyMasterData(user)
      myClubData <- initMyClubData(prematureUser.get)
      myPlayerData <- initMyPlayerData(myClubData)(prematureUser.get)
      leagueId <- initMyLeagueData(prematureUser.get)
    } yield userDataFusion(prematureUser, myClubData, myPlayerData, leagueId)

  }

  def login(username: String, pass: String, user: Option[User]): Future[User] = {
    user match {
      case Some(user) =>
        doLogin(u, p) map (res => {
          user.cookies = extractCookies(res)
          user
        })
      case _ =>
        doLogin(u, p) flatMap (res =>
          initiateUser(User(0, "", "", cookies = extractCookies(res))))
    }
  }

  private def doLogin(userName: String, pass: String): Future[HttpResponse] = {
    val pipeline: HttpRequest => Future[HttpResponse] = (sendReceive)
    val data = FormData(Map("userNameB" -> userName, "userPassWordB" -> pass))
    val request = Post("https://www.mytischtennis.de/community/login", data)

    pipeline(request)
  }

  def terminate() = system.terminate()

  private def addCookies(implicit user: User): HttpRequest => HttpRequest = {
    req: HttpRequest =>
      {
        if (user.cookies.isEmpty) req
        else addHeader(Cookie(user.cookies.values.toList))(req)
      }
  }

  private def extractCookies(resp: HttpResponse): Map[String, HttpCookie] = (resp.headers.collect { case `Set-Cookie`(cookie) => cookie } map (c => c.name -> c)).toMap

  private def extractAddCookies(implicit user: User): HttpResponse => HttpResponse = {
    res: HttpResponse =>
      {
        user.cookies ++= (res.headers.collect { case `Set-Cookie`(cookie) => cookie } map (c => c.name -> c)).toMap
        res
      }
  }

  private def checkRelogin(hres: HttpResponse)(implicit user: User): Boolean = {
    parser.isLoginPage(JsoupBrowser().parseString(hres.entity.asString)) match {
      case Some(b) if (b) => true
      case _ => false
    }
  }

  private def makeRequest(request: HttpRequest, retry: Boolean = true)(implicit user: User): Future[HttpResponse] = {
    val pipeline: HttpRequest => Future[HttpResponse] = (addCookies
      ~> addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      ~> addHeader("Accept-Language", "de,en-US;q=0.7,en;q=0.3")
      ~> addHeader("Accept-Encoding", "gzip")
      ~> encode(Gzip)
      ~> sendReceive
      ~> decode(Gzip)
      ~> extractAddCookies)

    // TODO: rewrite this ugly code
    val res = pipeline(request)
    if (retry) {
      res flatMap (hres =>
        if (checkRelogin(hres)) {
          doLogin(u, p) map (res => user.cookies = extractCookies(res))
          makeRequest(request, true)
        } else
          res)
    } else
      res
  }

  private def makeGetRequest(url: String)(implicit user: User): Future[HttpResponse] = makeRequest(Get(url))
  private def makePostRequest(url: String, form: FormData)(implicit user: User): Future[HttpResponse] = makeRequest(Post(url, form))

  // def makeCachedRequest(url: String) = cache(url) {
  //   makeGetRequest(url)
  // }

}
