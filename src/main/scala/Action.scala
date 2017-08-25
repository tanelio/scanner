import akka.actor.{Actor, Props}
import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by totala on 8/22/17.
  */
package action {

  import akka.actor.ActorRef
  import main.main._

  import scala.collection.mutable
  import scala.sys.process._

  object Actions {
    val actionmap = mutable.HashMap.empty[String, ActorRef]
    db.run(actions.result).map(_.foreach {
      case (id, action) =>
        actionmap += (id -> system.actorOf(Props[Action], action))
    })
    val iptablesprog = "iptables"
    val chain = "BLOCKED"
    def quoted(x: String) = "\"" + x + "\""

    Seq(iptablesprog, "-N", chain).!
    Seq(iptablesprog, "-A", chain, "-j", "LOG", "--log-prefix", quoted("BLOCKED: ")).!!  // --log-level 4
    Seq(iptablesprog, "-A", chain, "-j", "DROP").!!
  }

  case class ban(ip: String)
  case class unban(ip: String)

  class Action(val action: String) extends Actor {
    def receive = {
      case ban(ip) =>
        println(s"act = $ip")
      case unban(ip) =>
        println(s"unact = $ip")
    }
  }

}