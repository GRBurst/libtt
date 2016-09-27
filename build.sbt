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

// Tests //////////////////////////////

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.8.5" % "test")
scalacOptions in Test ++= Seq("-Yrangepos") // For specs2
