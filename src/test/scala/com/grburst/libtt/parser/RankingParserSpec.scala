package libtt.test

import org.specs2._

import com.grburst.libtt.Player
import com.grburst.libtt.parser.RankingParser

// class ClubRankingParserSpec extends Specification {
class RankingParserSpec extends org.specs2.mutable.Specification {

  "ranking parser" >> {

    val crp = RankingParser("src/test/res/myclub.htm")
    val lp = crp.get
    lp(0) mustEqual Player(100100, "1 / 1000", 1000, "Max Mustermann", "Musterverein", 200100, 2222 )
    lp(1) mustEqual Player(100200, "2 / 2000", 2000, "Maxi Musterfrau", "Musterverein", 200100, 2112 )

  }

}