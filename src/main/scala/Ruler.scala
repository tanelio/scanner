/**
  * Created by totala on 7/9/17.
  */
import java.sql.Timestamp

import main.main.{db, rules}
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

package ruler {

  import java.text.SimpleDateFormat

  import scala.collection.mutable
  import scala.util.matching.Regex

  object Ruler {

    var Rules = mutable.HashMap.empty[Int, (Regex, Int, Int, Int, Timestamp, Boolean)]

    // initialize rules from db
    db.run(rules.result).map(_.foreach {
      case (id, pattern, reps, findtime, bantime, started, active) =>
        if (active) {
          println(s"id#$id '$pattern' reps=$reps, findtime=$findtime")
          Rules += (id -> (pattern.r, reps, findtime, bantime, started, active))
        }
    })

    println(s"${Rules.size} rules loaded")

    /*
     * This should have worked... probably import collisions... slick 3.2.0 is a bit rough.
     *
    db.run(for (r <- rules if r.active === "Y") {
      println(r.pattern)
    })
    */
  }

  class date(x: String) {

    def date(x: String): String = {
      val mtht = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec"
      val mthft = "january|february|march|april|may|june|july|august|september|october|november|december"
      val wdayt = "mon|tues|wed|thur|fri|sat|sun"
      val wdayft = "monday|tuesday|wednesday|thursday|friday|saturday|sunday"
      val day = "[0-3][0-9]"
      val mth = "01|02|03|04|05|06|07|08|09|10|11|12"
      val year = "19[7-9][0-9]|20[0-3][0-9]"
      ""
   }
  }

  class tokenizer(x: String)
  {
    val splits = " ,:.".toArray
    x.split(splits).filter(_.nonEmpty)
  }

  class parse(x: String) {
    import ruler.Ruler.Rules
    private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")

    // Take x, parse date, get the lenth of date parsed as offset
    // run, rules against the rest, instantiate (or call) FSMs as needed.

    // RuleFSM:
    //  - if key'ed off sourceIP, then instantiate another for every new IP
    //  - the FSM needs to have the IP & timestamp
    //  - rule should define whether sourceIP is the key

    val dt = OLD_SYSLOG_DATE_FORMAT.parse(x) // 15+1 characters for date
    val host = x.drop(16).takeWhile(! _.isSpaceChar)
    val str = x.drop(16 + host.length + 1)
    for ((id, pat, reps, ft, bt, st, active) <- Rules if active) {
      str match {
        case pat =>
      }
    }
  }

}