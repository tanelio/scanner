/**
  * Created by totala on 7/9/17.
  */
import main.main.{attacks, db, rules}
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

package ruler {

  import java.net.InetAddress._
  import java.sql.Timestamp
  import java.text.SimpleDateFormat

  import akka.actor.Actor
  import com.google.common.net.InetAddresses.{coerceToInteger, forString}

  import scala.collection.mutable
  import scala.util.matching.Regex

  object Ruler {

    var Rules = mutable.HashMap.empty[Int, (Regex, Int, Int, Int, Boolean)]
    type Inst = mutable.HashMap[Int, (Long, Int)]
    var ruleInst = mutable.HashMap.empty[Int, Inst]
    val ipv4 = "(\\d+\\.\\d+\\.\\d+\\.\\d+)"

    // initialize rules from db
    db.run(rules.result).map(_.foreach {
      case (id, pattern, reps, findtime, bantime, active) =>
        if (active) {
          println(s"id#$id '$pattern' reps=$reps, findtime=$findtime")
          new rule(id, new Regex(pattern.replaceAllLiterally("$ipv4", ipv4)), reps, findtime, bantime)
          println("=> ", pattern.replaceAllLiterally("$ipv4", ipv4))
        }
    })

    println(s"${Rules.size} rules loaded")

    /*
     * This should have worked... probably import conflict... slick 3.2.0 is a bit rough.
     *
    db.run(for (r <- rules if r.active === "Y") {
      println(r.pattern)
    })
    */

    case class Line(l: String, dt: Long, host: String, str: String)
    class rule(val id: Int, val pat: Regex, val reps: Int, val findtime: Int, val bantime: Int) extends Actor {
      val instances = mutable.HashMap.empty[Int, Int]
      def receive = {
        case Line(l, dt, host, str) =>
          l match {
            case pat(ips) =>
              val ip = coerceToInteger(forString(ips))
              attacks += (0, new Timestamp(dt), ip, getByName(ips).isSiteLocalAddress, host, 0, 0, str)
          }
      }
    }

    private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")

    def parse(x: String) {
      import java.sql.Timestamp
      import com.google.common.net.InetAddresses._

      // Take x, parse date, get the lenth of date parsed as offset
      // run, rules against the rest, instantiate (or call) FSMs as needed.

      // RuleFSM:
      //  - if key'ed off sourceIP, then instantiate another for every new IP
      //  - the FSM needs to have the IP & timestamp
      //  - rule should define whether sourceIP is the key

      val dt = OLD_SYSLOG_DATE_FORMAT.parse(x).getTime // 15+1 characters for date
      val host = x.drop(16).takeWhile(!_.isSpaceChar)
      val str = x.drop(16 + host.length + 1)
      val now = System.currentTimeMillis()
      for ((id, (pat, reps, ft, bt, active)) <- Rules if active) {
        str match {
          case pat(ips) => // We know the id, and the ip... hunt down the instance
            val ip = coerceToInteger(forString(ips))
            attacks += (0, new Timestamp(dt), ip, getByName(ips).isSiteLocalAddress, host, 0, 0, str)
            if (ruleInst.contains(id)) {
              if (ruleInst(id).contains(ip)) {
                ruleInst(id) = ruleInst.getOrElse(id, mutable.HashMap(ip -> (dt, 0)))
              }
            } else
              ruleInst(id) += (ip -> (dt, 0))
        }
      }
    }

    def prune: Unit = {

    }

  }

  /*
   * Future: build a date recognizer/adjusting configurator
   *
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
  */

}
