/**
  * Created by totala on 7/9/17.
  */
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

package ruler {

  import java.net.InetAddress._
  import java.sql.Timestamp
  import java.text.SimpleDateFormat

  import action.{Actions, Ban}
  import akka.actor.{Actor, ActorRef, Props, Terminated}
  import akka.util.ByteString
  import com.google.common.net.InetAddresses.{coerceToInteger, forString}
  import main.main._

  import scala.collection.mutable
  import scala.util.matching.Regex
  import scala.concurrent.duration._

  object Ruler {
    val ipv4 = "(\\d+\\.\\d+\\.\\d+\\.\\d+)"

    val rulerref = system.actorOf(Props[ruler])
    private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")

    class ruler extends Actor {
      import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}
      import action.Actions._
      Actions.init
      var router = {
        val routees = Vector.empty[ActorRefRoutee]
        Router(BroadcastRoutingLogic(), routees)
      }
      db.run(rules.result).map(_.foreach {
        case (id, preamble, pattern, reps, findtime, bantime, active, action) =>
          if (active) {
            println(s"id#$id '$pattern' reps=$reps, findtime=$findtime")
            // val r = context.actorOf(Props(new rule(id, new Regex(pattern.replaceAllLiterally("$ipv4", ipv4)), reps, findtime, bantime)))
            //   val props = Props(classOf[MyActor], arg1, arg2)
            val r = context.actorOf(Props(classOf[rule], id, regexconv(preamble), regexconv(pattern), reps, findtime, bantime, actionmap(action)))
            context watch r
            router = router.addRoutee(r)
            //new rule(id, new Regex(pattern.replaceAllLiterally("$ipv4", ipv4)), reps, findtime, bantime)
            println("=> " + pattern.replaceAllLiterally("$ipv4", ipv4))
          }
      })
      println(s"${router.routees.size} rules loaded")
      val pruneperiodic = system.scheduler.schedule(60 seconds, 60 seconds, rulerref, Prune)

      def regexconv(x: String) : Regex = new Regex(x.replaceAllLiterally("$ipv4", ipv4))

      def receive = {
        // Syslog format: https://en.wikipedia.org/wiki/Syslog
        // Syslog RFC: https://tools.ietf.org/html/rfc5424
        // SDF: https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
        case x: ByteString =>   // Let's process as much as makes sense for all rules
          val str = x.utf8String.dropWhile(_ != '>').drop(1) // Take out PRI <xxx>
          println(s"Line = $str")
          val dt = OLD_SYSLOG_DATE_FORMAT.parse(str).getTime // 15+1 characters for date
        //          val now = System.currentTimeMillis()    // alternative: take current timestampt
          val host = str.drop(16).takeWhile(!_.isSpaceChar)
          router.route(Line(str, dt, host, 16 + host.length + 1), sender())
        case Terminated(a) =>
          // Todo: re-instate rule actor?
        case Prune =>
          router.route(Prune, rulerref)
      }
      pruneperiodic.cancel()
    }

    case class Line(l: String, dt: Long, host: String, off: Int)
    case class Prune()
    class rule(val id: Int, val pre: Regex, val pat: Regex, val reps: Int, val findtime: Int, val bantime: Int, val actionref: ActorRef) extends Actor {
      println(s"Actor id#$id started\n  pre=$pre\n  pat=$pat")
      val instances = mutable.HashMap.empty[Int, Int]   // IP, repetitions
      var preseen = 0L  // ToDo: make preamble/pat work for multiple hosts
      val preAmble = !pre.toString.isEmpty
      if (!preAmble)
        context.become(pattern)

      def prune = {
        // Todo: Go through all IPs, see if findtime has been exceeded
      }

      def receive = {
        case Line(l, dt, host, off) =>
          l.drop(off) match {
            case pre(str) =>
              println(s"preAmble seen: $l, str=$str")
              preseen = dt
              db.run(DBIO.seq(
                attacks += (0, new Timestamp(dt), -1, false, host, 0, 0, l)))
              context.become(pattern) // PreAmble seen, switch to pattern mode
            case _ =>
          }
        case Prune =>
          prune
          // todo: implement unban?
      }

x      def pattern: Receive = {
        case Line(l, dt, host, off) =>
          // preamble defined, but seen more than 5 sec (5000 ms) ago => look for more preamble
          if (preAmble && preseen < dt - 5000)
            context.unbecome
          l.drop(off) match {
            case pat(ips) =>
              val ip = coerceToInteger(forString(ips))
              println(s"MATCH id#$id, IP=$ips Line=$l")
              // ToDo: implement reps & action
              actionref ! Ban(ips)
              db.run(DBIO.seq(
                attacks += (0, new Timestamp(dt), ip, getByName(ips).isSiteLocalAddress, host, 0, 0, l)))
              if (preAmble)
                context.unbecome  // alert occurred, look for preamble again
            case _ =>
          }
        case Prune =>
          prune
      }
    }
  }

}
