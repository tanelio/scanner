/**
  * Created by totala on 7/9/17.
  */
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

package ruler {

  import java.net.InetAddress._
  import java.sql.Timestamp
  import java.text.SimpleDateFormat

  import akka.actor.{Actor, Props, Terminated}
  import akka.util.ByteString
  import com.google.common.net.InetAddresses.{coerceToInteger, forString}
  import main.main._

  import scala.collection.mutable
  import scala.util.matching.Regex

  object Ruler {
    val ipv4 = "(\\d+\\.\\d+\\.\\d+\\.\\d+)"

    val rulerref = system.actorOf(Props[ruler])
    private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")

    class ruler extends Actor {
      import akka.routing.{ActorRefRoutee, BroadcastRoutingLogic, Router}
      var router = {
        val routees = Vector.empty[ActorRefRoutee]
        Router(BroadcastRoutingLogic(), routees)
      }
      db.run(rules.result).map(_.foreach {
        case (id, pattern, reps, findtime, bantime, active) =>
          if (active) {
            println(s"id#$id '$pattern' reps=$reps, findtime=$findtime")
            // val r = context.actorOf(Props(new rule(id, new Regex(pattern.replaceAllLiterally("$ipv4", ipv4)), reps, findtime, bantime)))
            //   val props = Props(classOf[MyActor], arg1, arg2)
            val r = context.actorOf(Props(classOf[rule], id, new Regex(pattern.replaceAllLiterally("$ipv4", ipv4)), reps, findtime, bantime))
            context watch r
            router = router.addRoutee(r)
            //new rule(id, new Regex(pattern.replaceAllLiterally("$ipv4", ipv4)), reps, findtime, bantime)
            println("=> " + pattern.replaceAllLiterally("$ipv4", ipv4))
          }
      })
      println(s"${router.routees.size} rules loaded")
      def receive = {
        case x: ByteString =>
          val str = x.utf8String.dropWhile(_ != '>').drop(1) // Take out PRI <xxx>
          println(s"Line = $str")
          val dt = OLD_SYSLOG_DATE_FORMAT.parse(str).getTime // 15+1 characters for date
          val host = str.drop(16).takeWhile(!_.isSpaceChar)
          val off = 16 + host.length + 1
//          val now = System.currentTimeMillis()
//          router.route(w, sender())
//          routees ! Line(str, dt, host, off)
//          router ! Line(str, dt, host, off)
//          router ! Broadcast("test")
          router.route(Line(str, dt, host, off), sender())
        case Terminated(a) =>
      }
    }

    case class Line(l: String, dt: Long, host: String, off: Int)
    case class Prune()
    class rule(val id: Int, val pat: Regex, val reps: Int, val findtime: Int, val bantime: Int) extends Actor {
      println(s"Actor id#$id started")
      val instances = mutable.HashMap.empty[Int, Int]   // IP, repetitions
      def receive = {
        case Line(l, dt, host, off) =>
//          println(s"Actor id#$id received '$l'")
          l.drop(off) match {
            case pat(ips) =>
              val ip = coerceToInteger(forString(ips))
              println(s"MATCH id#$id, IP=$ips Line=$l")
              val insertActions = DBIO.seq(
                attacks += (0, new Timestamp(dt), ip, getByName(ips).isSiteLocalAddress, host, 0, 0, l)
              )
              db.run(insertActions)
            case _ =>
          }
        case Prune =>

      }
    }
  }


}
