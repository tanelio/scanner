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

  object Ruler {

    var Rules = mutable.HashMap.empty[Int, (String, Int, Int, Int, Timestamp)]

    // initialize rules from db
    db.run(rules.result).map(_.foreach {
      case (id, pattern, reps, findtime, bantime, started, active) =>
        if (active) {
          println(s"id#$id '$pattern' reps=$reps, findtime=$findtime")
          Rules += (id -> (pattern, reps, findtime, bantime, started))
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
    private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")
  }

}