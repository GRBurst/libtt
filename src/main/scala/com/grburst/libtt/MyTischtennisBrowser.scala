package com.grburst.libtt

import com.grburst.libtt.parser.MyTischtennisParser
import com.grburst.libtt.types._
import com.grburst.libtt.util.types._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Success, Failure }
import scala.concurrent.Await
import scala.concurrent.duration._
// import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.MediaRanges.`*/*`
import akka.http.scaladsl.model.Uri._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.headers.HttpEncodings._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.event.Logging

import com.typesafe.config.ConfigFactory

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

// case class StateManager(user: User, credentials: Credientials)

// case class MyTischtennisBrowser(private val username: String, private val pass: String)(implicit val ec: ExecutionContext) {
// object MyTischtennisBrowser {
case class MyTischtennisBrowser(implicit val ec: ExecutionContext) {
  private val parser = MyTischtennisParser()

  // akka.http.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"
  // akka.http.???.max-redirects = 0
  private val libttConf = ConfigFactory.parseString("""
    akka.http.client.user-agent-header = "Mozilla/5.0 (X11; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0"
    """)

  implicit val system = ActorSystem("libtt-actors", libttConf)
  implicit val materializer = ActorMaterializer()
  val log = Logging.getLogger(system, this)
  // import system.dispatcher

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
      .withQuery(Query("showmyclub" -> "1"))

    makeGetRequest(clubRankingUrl.toString) map (doc => parser.parseRanking(doc).getOrElse(Nil))
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
      .withQuery(Query(
        "writetodb" -> "false",
        "otherUsersClickTTId" -> playerId.toString,
        "otherUsersUserId" -> "0",
        "nameOfOtherUser" -> (playerFirstname + " " + playerSurname)))

    makeGetRequest(playerProfileUrl.toString) map (doc => parser.parsePlayerProfile(doc))
  }

  def getPlayerEvents(playerId: Option[Int] = None, timeInterval: Option[String] = Some("last12Months"))(implicit user: User): Future[List[Event]] = {
    val playerEventsUrl = Uri("https://www.mytischtennis.de/community/events")
    val request = timeInterval match {
      case None => playerId match {
        case None => Get(playerEventsUrl) //HttpRequest(HttpMethods.GET, playerEventsUrl)
        case Some(player) => Get(playerEventsUrl.withQuery(Query("personId" -> player.toString))) //HttpRequest(HttpMethods.GET, playerEventsUrl.withQuery(Query("personId" -> player.toString)))
      }
      case Some(time) =>
        val data = FormData(Map("timeIntervalKeyWord" -> time))
        playerId match {
          case None => Post(playerEventsUrl, data.toEntity) //HttpRequest(HttpMethods.POST, playerEventsUrl, Nil, data.toEntity)
          case Some(player) => Post(playerEventsUrl.withQuery(Query("personId" -> player.toString)), data.toEntity) //HttpRequest(HttpMethods.POST, playerEventsUrl.withQuery(Query("personId" -> player.toString)), Nil, data.toEntity)
        }
    }

    makeRequest(request) map (doc => parser.parseEvents(doc).getOrElse(Nil))
  }

  // https://www.mytischtennis.de/community/ranking?showgroupid=275617
  def getLeagueRanking(leagueId: Int)(implicit user: User): Future[List[Player]] = {
    val leagueUrl = Uri("https://www.mytischtennis.de/community/ajax/_rankingList")
      .withQuery(Query(
        "groupid" -> leagueId.toString,
        "ttrQuartalorAktuell2" -> "aktuell"))

    makeGetRequest(leagueUrl.toString) map (doc => parser.parseRanking(doc).getOrElse(Nil))
  }

  def getClubRanking(club: Club)(implicit user: User): Future[List[Player]] = {
    searchCustomRanking(Map("alleSpielberechtigen" -> "YES", "verband" -> "Alle",
      "verein" -> club.name,
      "vereinId" -> (club.id.toString + "," + club.organisation)))
  }

  def getEventDetail(eventId: Int)(implicit user: User): Future[List[MyMatch]] = {
    val eventDetailUrl = Uri("https://www.mytischtennis.de/community/eventDetails")
      .withQuery(Query("fq" -> "false", "eventId" -> eventId.toString))

    makeGetRequest(eventDetailUrl.toString) map (doc => parser.parseEventDetail(doc).getOrElse(Nil))
  }

  def searchClub(clubName: String)(implicit user: User): Future[List[Club]] = {
    val searchClubUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Query("trigger" -> "1", "d" -> (clubName)))

    makeGetRequest(searchClubUrl.toString) map (doc => parser.parseSearchClubList(doc).getOrElse(Nil))
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
      .withQuery(Query(Map(
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
        "showmyfriends" -> "0") ++ userChoice))

    makeGetRequest(searchRankingUrl.toString) map (doc => playerParser(doc).getOrElse(Nil))
  }

  def login(username: String, pass: String, user: Option[User] = None): Future[User] = {
    user match {
      case Some(user) =>
        doLogin(username, pass) map (res => {
          user.cookies = extractCookies(res)
          res.discardEntityBytes()
          user
        })
      case _ =>
        doLogin(username, pass) flatMap (res => {
          val c = extractCookies(res)
          res.discardEntityBytes()
          initiateUser(User(0, "", "", cookies = c))
        })
    }
  }

  def terminate() = system.terminate()

  private def initMyMasterData(implicit user: User): Future[Option[User]] = {
    val userUrl = "https://www.mytischtennis.de/community/userMasterPagePrint"

    makeGetRequest(userUrl) map (doc => {
      val tmpUser = parser.parseUserMasterPage(doc)
      if (tmpUser.isDefined) tmpUser.get.cookies ++= user.cookies
      tmpUser
    })
  }

  private def initMyClubData(implicit user: User): Future[Option[Club]] = {
    val searchUrl = Uri("https://www.mytischtennis.de/community/ajax/_vereinbox")
      .withQuery(Query("trigger" -> "1", "d" -> (user.club.get)))

    makeGetRequest(searchUrl.toString) map (doc => Try(parser.parseSearchClubList(doc).get.head).toOption)
  }

  private def initMyLeagueData(implicit user: User): Future[Option[Int]] = {
    val leagueUrl = "https://www.mytischtennis.de/community/group"

    makeGetRequest(leagueUrl) map (doc => parser.findGroupId(doc))
  }

  private def initMyPlayerData(club: Option[Club])(implicit user: User): Future[Option[Player]] = {
    (searchPlayer(user.firstname, user.surname, club.get.id, club.get.name, user.organisation.get)) map (l => Try(l.head).toOption)
  }

  private def userDataFusion(user: Option[User], club: Option[Club], player: Option[Player], leagueId: Option[Int]): User = {

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
  // Step 1: parse users master page  => Get firstname, surname, club, organisation, league, qttr
  // Step 2: search my club           => Get clubId, clubName, organisation, orgaId
  // Step 3: search player (self)     => Get id, vRank, dRank, fullName, club, clubId, ttr
  // Step 4: search group info page   => Get leagueId
  private def initiateUser(user: User): Future[User] = {
    for {
      prematureUser <- initMyMasterData(user)
      myClubData <- initMyClubData(prematureUser.get)
      myPlayerData <- initMyPlayerData(myClubData)(prematureUser.get)
      leagueId <- initMyLeagueData(prematureUser.get)
    } yield userDataFusion(prematureUser, myClubData, myPlayerData, leagueId)

  }

  private def doLogin(username: String, pass: String): Future[HttpResponse] = {

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri("https://www.mytischtennis.de/community/login"),
      entity = FormData(Map("userNameB" -> username, "userPassWordB" -> pass)).toEntity)

    Http().singleRequest(request)
  }

  private def addCookies(req: HttpRequest)(implicit user: User): HttpRequest = {
    if (!user.cookies.isEmpty) req.addHeader(Cookie(cookies = (user.cookies.values map (cookie => cookie.pair)).toList))
    else req
  }

  private def extractCookies(resp: HttpResponse): Map[String, HttpCookie] = resp.headers.collect { case `Set-Cookie`(cookie) => cookie.name -> cookie }.toMap

  private def extractAddCookies(resp: HttpResponse)(implicit user: User): HttpResponse = {
    user.cookies ++= extractCookies(resp)
    resp
  }

  private def checkRelogin(doc: HttpDoc)(implicit user: User): Boolean = {
    parser.isLoginPage(doc) match {
      case Some(b) if b => true
      case _ => false
    }
  }

  private def makeRequest(request: HttpRequest, retry: Boolean = true)(implicit user: User): Future[HttpDoc] = {
    val finalRequest = addCookies(request)
      .addHeader(Accept(`text/html`, `application/xhtml+xml`, `application/xml` withQValue 0.9f, `*/*` withQValue 0.8))
      .addHeader(`Accept-Language`(Language("de"), Language("en-US") withQValue 0.7f, Language("en") withQValue 0.3f))
      .addHeader(`Accept-Encoding`(gzip))
      .addHeader(`Content-Encoding`(gzip))

    val response = Http().singleRequest(Gzip.encode(finalRequest)) map (res => Gzip.decode(res))

    val doc = response map (resp => extractAddCookies(resp)) flatMap (resp => Unmarshal(resp).to[String]) map (str => JsoupBrowser().parseString(str))
    doc

    // TODO: Relogin (below is pseudocode)
    // if (checkRelogin(d) && retry) {
    //   login(Some(user))
    //   makeRequest(request, false)
    // }

  }

  private def makeGetRequest(url: String)(implicit user: User): Future[HttpDoc] = makeRequest(Get(url)) //makeRequest(HttpRequest(method = HttpMethods.GET, uri = Uri(url)))
  private def makePostRequest(url: String, form: FormData)(implicit user: User): Future[HttpDoc] = makeRequest(Post(url, form.toEntity)) //makeRequest(HttpRequest(method = HttpMethods.POST, uri = Uri(url), entity = form.toEntity))

}
