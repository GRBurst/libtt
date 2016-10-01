name := "libtt"


scalaVersion := "2.11.8"
scalacOptions ++=  (
  "-encoding" :: "UTF-8" ::
  "-unchecked" ::
  "-deprecation" ::
  "-explaintypes" ::
  "-feature" ::
  "-language:_" ::
  "-Xlint:_" ::
  "-Ywarn-unused"::
  // "-Xdisable-assertions" ::
  // "-optimize" ::
  // "-Yopt:_" :: // enables all 2.12 optimizations
  // "-Yinline" :: "-Yinline-warnings" ::
  Nil)

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.0.0"
libraryDependencies ++= Seq("io.spray" %% "spray-caching" % "1.3.2",
  "io.spray" %% "spray-client" % "1.3.2",
  "com.typesafe.akka"   %%  "akka-actor"    % "2.4.10")
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ws" % "2.5.8",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
//  "com.github.scopt" %% "scopt" % "3.5.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
// libraryDependencies ++= Seq("io.spray" %% "spray-client" % "1.3.2",
  // "com.typesafe.akka"   %%  "akka-actor"    % "2.3.6",
  // "com.typesafe.akka"   %%  "akka-testkit"  % "2.3.6"   % "test",
  // "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test",
  // "org.scalaz"          %%  "scalaz-core"   % "7.1.0")

initialCommands in console := """
import com.grburst.libtt
import com.grburst.libtt._
import com.grburst.libtt.util.types._
import com.grburst.libtt.parser.MyTischtennisParser
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }

import spray.client.pipelining._
import spray.http.{ FormData, HttpCookie, HttpRequest, HttpResponse }
import spray.http.Uri
import spray.http.HttpHeaders.{ Cookie, `Set-Cookie` }
import spray.httpx.encoding.Gzip

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
"""

// Tests //////////////////////////////

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.8.5" % "test")
scalacOptions in Test ++= Seq("-Yrangepos") // For specs2
