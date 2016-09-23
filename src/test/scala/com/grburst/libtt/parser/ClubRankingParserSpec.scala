package libtt.test

import org.specs2._

import com.grburst.libtt.Player
import com.grburst.libtt.parser.ClubRankingParser

// class ClubRankingParserSpec extends Specification {
class ClubRankingParserSpec extends org.specs2.mutable.Specification {

  "club ranking parser" >> {

    val crp = ClubRankingParser("src/test/res/myclub.htm")
    val lp = crp.get
    lp(0) mustEqual Player(100100, "1 / 1000", 1000, "Max Mustermann", "Musterverein", 200100, 2222 )
    lp(1) mustEqual Player(100200, "2 / 2000", 2000, "Maxi Musterfrau", "Musterverein", 200100, 2112 )

  }

}
