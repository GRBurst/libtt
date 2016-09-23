package libtt.test

import org.specs2._

import com.grburst.libtt.{MyMatch}
import com.grburst.libtt.parser.EventDetailParser

class EventDetailParserSpec extends org.specs2.mutable.Specification {

  "event detail parser" >> {

    val edp = EventDetailParser("src/test/res/eventDetails.htm")
    val lm = edp.get
    lm(0).opponent mustEqual "Mustermann1, Max1"
    lm(0).oTTR.get mustEqual 2000
    lm(0).oId mustEqual 2000100
    lm(0).result mustEqual "3:0"
    lm(0).set1 mustEqual "11:5"
    lm(0).set2 mustEqual "11:6"
    lm(0).set3 mustEqual "11:7"
    lm(0).set4 mustEqual None
    lm(0).set5 mustEqual None
    lm(0).set6 mustEqual None
    lm(0).set7 mustEqual None
    lm(0).ge mustEqual 0.511f

    lm(0) mustEqual MyMatch("Mustermann1, Max1", Some(2000), 2000100, "3:0", "11:5", "11:6", "11:7", None, None, None, None, 0.511f )

  }

}
