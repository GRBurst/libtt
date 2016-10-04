lazy val commonSettings = Seq(
  name := "libtt",
  organization := "com.grburst",
  version := "0.3.0-SNAPSHOT",
  scalaVersion := "2.11.8")

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= (
      "-encoding" :: "UTF-8" ::
      "-unchecked" ::
      "-deprecation" ::
      "-explaintypes" ::
      "-feature" ::
      "-language:_" ::
      "-Xlint:_" ::
      "-Ywarn-unused" ::
      // "-Xdisable-assertions" ::
      // "-optimize" ::
      // "-Yopt:_" :: // enables all 2.12 optimizations
      // "-Yinline" :: "-Yinline-warnings" ::
      Nil),

    libraryDependencies ++= Seq(
      "net.ruippeixotog" %% "scala-scraper" % "1.0.0",
      "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11",
      "com.typesafe.akka" %% "akka-actor" % "2.4.11",
      "com.typesafe" % "config" % "1.3.1"),

    initialCommands in console := """
import com.grburst.libtt
import com.grburst.libtt._
import com.grburst.libtt.types._
import com.grburst.libtt.util.types._
import com.grburst.libtt.parser.MyTischtennisParser
import com.grburst.libtt.util.parsingHelper.StringHelper

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }


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
import com.typesafe.config.ConfigFactory

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
""",

    // Tests //////////////////////////////
    libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.8.5" % "test"),
    scalacOptions in Test ++= Seq("-Yrangepos") // For specs2

  )
