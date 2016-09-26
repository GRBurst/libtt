package com.grburst.libtt

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}
import scala.collection.mutable

import spray.caching.{LruCache, Cache}
import spray.http._
import spray.client.pipelining._
import spray.http.HttpHeaders._
import spray.httpx.encoding.{Gzip, Deflate}

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import com.grburst.libtt.util.types._

import scala.concurrent.Await
import scala.concurrent.duration._


case class MyTischtennisBrowser() {

  private val libttConf = ConfigFactory.parseString("""
    spray.can.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"
    spray.can.host-connector.max-redirects = 0"""
  )

  implicit val system = ActorSystem("libtt-actors", libttConf)
  import system.dispatcher

  def login(user:String, pass:String) = {

    val l = doLogin(u, p)
    l onComplete {
      case Success(res) => {
        cookieStorage ++= extractCookies(res)
        println("Successfully logged in")
      }
      case Failure(e) => println(s"Error: $e")
    }
    // val res = Await.result(l, Duration.Inf)

  }

  // def getMyEvents: Future[List[Event]] = {
  def getMyEvents = {
    val eventsUrl = "https://www.mytischtennis.de/community/events"
    val eventResponse = makeRequest(eventsUrl)

    eventResponse onComplete {
      case Success(hres) => {
        if(hres.entity.nonEmpty){
          println("Entity not empty")
          val browser = JsoupBrowser()
          val l = parser.parseEvents(browser.parseString(hres.entity.asString))
          println(s"Event List = $l")
        } else {
          println("Entity empty")
        }
      }
      case Failure(e) => println(s"Error: $e")
    }

  }

  def debugPrintCookies = println(s"cookies stored = ${cookieStorage.toString}")
  // def searchClub(clubName: String): List[Club] = {}
  // def searchPlayer(firstName: String, lastName): Club = {}

  // def getClubById(clubId: Int, clubName: String): Club = {}
  // def getEventById(eventId: Int): Event = {}
  // def getMatchById(matchId: Int): MyMatch = {}
  // def getPlayerById(playerId: Int): Player = {}

  private val cookieStorage: mutable.ArrayBuffer[HttpCookie] = new mutable.ArrayBuffer()
  // val cache: Cache[HttpResponse] = LruCache()

  // spray.can.client.user-agent-header = "Mozilla/5.0 (X11; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0"
  // spray.can.client.user-agent-header = "Dalvik/2.1.0 (Linux; U; Android 6.0.1;)"

  // private def initiateUser = {
  //   val url = "https://www.mytischtennis.de/community/userMasterPagePrint"
  // }

  private def addCookies(): HttpRequest => HttpRequest = {
    req: HttpRequest => {
      if (cookieStorage.isEmpty) req
      else addHeader(Cookie(cookieStorage))(req)
    }
  }

  private def extractCookies(resp: HttpResponse): List[HttpCookie] = resp.headers.collect { case `Set-Cookie`(cookie) => cookie }

  private def doLogin(user: String, pass: String): Future[HttpResponse] = {

    val pipeline: HttpRequest => Future[HttpResponse] = (sendReceive)

    val data = FormData(Map("userNameB" -> user, "userPassWordB" -> pass))
    val request = Post("https://www.mytischtennis.de/community/login", data)

      pipeline(request)

  }

  private def makeRequest(url: String):Future[HttpResponse] = {

    val pipeline: HttpRequest => Future[HttpResponse] = (addCookies
      ~> encode(Gzip)
      ~> sendReceive
      ~> decode(Deflate))

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
