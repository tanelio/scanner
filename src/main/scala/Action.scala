import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by totala on 8/22/17.
  */
package action {

  import akka.actor.{Actor, ActorRef, Props}
  import main.main._

  import scala.collection.mutable
  import scala.sys.process._

  object Actions {
    val actionmap = mutable.HashMap.empty[String, ActorRef]
    def init = {
      db.run(actions.result).map(_.foreach {
        case (id, action) =>
          actionmap += (id -> system.actorOf(Props[Action], action))
      })
    }
    val chain = "BLOCKED"
    def Quote(x: String) = "\"" + x + "\""
    var banned = mutable.HashSet.empty[String]

    // todo: handle already established chain
    Seq(sudoprog, iptablesprog, "-N", chain).!!
    // todo: should logging be limited to 1/sec to avoid DoS?
    Seq(sudoprog, iptablesprog, "-A", chain, "-j", "LOG", "--log-prefix", Quote("BLOCKED: ")).!!  // --log-level 4
    Seq(sudoprog, iptablesprog, "-A", chain, "-j", "DROP").!!
    // todo: pre-load banned with already banned IPs from iptables
    val blocks = Seq(sudoprog, iptablesprog, "-vnL", chain).!!
    //   748 45088 DROP       all  --  *      *       5.230.4.124          0.0.0.0/0
    val pattern = """\d+\s+\d+\s+\w+\s+.+(\d+\.\d+\.\d+\.\d+)\s+.+""".r
    // todo: convert into a stream? unlikely that string length can be a problem
    for (l <- blocks.split("\n"))
      l.trim match {
        case pattern(ip) =>
          banned += ip
        // todo: default case missing
      }
  }

  case class Ban(ip: String)
  case class Unban(ip: String)

  class Action(val action: String) extends Actor {
    import Actions._

    def receive = {
      case Ban(ip) =>
        if (banned.contains(ip))
          println(s"already banned $ip")
        else {
          banned += ip
          println(s"ban = $ip")
          Seq(sudoprog, iptablesprog, "-A", "INPUT", "-s", ip, "-j", chain).!!
        }
      case Unban(ip) =>
        if (banned.contains(ip)) {
          banned -= ip
          println(s"unban = $ip")
          Seq(sudoprog, iptablesprog, "-D", "INPUT", "-s", ip).!!
        } else
          println(s"already unbanned $ip")
    }
  }
}
