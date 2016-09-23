package libtt.test

import org.specs2._

import com.grburst.libtt.Club
import com.grburst.libtt.parser.ClubSearchParser

class ClubSearchParserSpec extends org.specs2.mutable.Specification {

  "club search parser" >> {

    val csp = ClubSearchParser("src/test/res/verein_search.htm")
    val lc = csp.get
    lc(0) mustEqual Club(10100, "TTV Musterclub 1", "XTTV", 1010 )
    lc(1) mustEqual Club(100100, "TTV Musterclub 2", "YTTV", 1020 )
    lc(2) mustEqual Club(1000100, "TTV Musterclub 3", "XTTV", 1030 )


  }

}
