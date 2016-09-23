name := "libtt"


scalaVersion := "2.11.8"

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "1.0.0"
libraryDependencies ++= Seq("io.spray" %% "spray-client" % "1.3.2",
  "com.typesafe.akka"   %%  "akka-actor"    % "2.4.10")
// libraryDependencies ++= Seq("io.spray" %% "spray-client" % "1.3.2",
  // "com.typesafe.akka"   %%  "akka-actor"    % "2.3.6",
  // "com.typesafe.akka"   %%  "akka-testkit"  % "2.3.6"   % "test",
  // "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test",
  // "org.scalaz"          %%  "scalaz-core"   % "7.1.0")

// Tests //////////////////////////////

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.8.5" % "test")
scalacOptions in Test ++= Seq("-Yrangepos")
