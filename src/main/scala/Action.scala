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
      //val actionref = system.actorOf(Props[Action], action)
      db.run(actions.result).map(_.foreach {
        case (id, action) =>
          actionmap += (id -> system.actorOf(Props[Action], action))
      })
    }
    val chain = "BLOCKED"
    def quoted(x: String) = "\"" + x + "\""
    def ban(id: String, ip :String) = actionmap(id) ! Ban(ip)

    Seq(iptablesprog, "-N", chain).!!
    Seq(iptablesprog, "-A", chain, "-j", "LOG", "--log-prefix", quoted("BLOCKED: ")).!!  // --log-level 4
    Seq(iptablesprog, "-A", chain, "-j", "DROP").!!
  }

  case class Ban(ip: String)
  case class Unban(ip: String)

  class Action(val action: String) extends Actor {
    import Actions._
    def receive = {
      case Ban(ip) =>
        println(s"act = $ip")
        Seq(iptablesprog, "-A", "INPUT", "-s", ip, "-j", chain).!!
      case Unban(ip) =>
        println(s"unact = $ip")
        Seq(iptablesprog, "-D", "INPUT", "-s", ip).!!
    }
  }

}