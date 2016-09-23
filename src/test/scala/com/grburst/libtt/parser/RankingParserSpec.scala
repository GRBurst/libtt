package libtt.test

import org.specs2._

import com.grburst.libtt.Player
import com.grburst.libtt.parser.RankingParser

// class ClubRankingParserSpec extends Specification {
class RankingParserSpec extends org.specs2.mutable.Specification {

  "ranking parser" >> {

    val crp = RankingParser("src/test/res/myclub.htm")
    val lp = crp.get
    lp(0).id mustEqual 100100
    lp(0).vRank.get mustEqual 1
    lp(0).dRank.get mustEqual 1000
    lp(0).name mustEqual "Max Mustermann"
    lp(0).club mustEqual "Musterverein"
    lp(0).clubId mustEqual 200100
    lp(0).ttr.get mustEqual 2222
    lp(0) mustEqual Player(100100, Some(1), Some(1000), "Max Mustermann", "Musterverein", 200100, Some(2222) )
    lp(1) mustEqual Player(100200, Some(2), Some(2000), "Maxi Musterfrau", "Musterverein", 200100, Some(2112) )

  }

}
