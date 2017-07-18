import java.sql.Timestamp

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory

//import akka.event.Logging
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

import scala.sys.process._

package main {

  import ruler.Ruler
  import slick.dbio.DBIOAction
  import syslog.SyslogReceiver

  import scala.concurrent.Await

  /**
    * Created by totala on 6/13/17.
    */

  // Evils: Super | Genius | Resident | Casual | Happles
  class Attacks(tag: Tag) extends Table[(Int, Timestamp, Int, Char, Int, Int, Int, String)](tag, "ATTACKS") {
    def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
    def ts = column[Timestamp]("TS") // When incident occurred
    def sip = column[Int]("SIP") // SourceIP
    def ll = column[Char]("LL") // LinkLocal [Y|N] don't scan locals
    def dip = column[Int]("DIP") // DestinationIP
    def dport = column[Int]("DPORT") // Destination Port
    def evil = column[Int]("TYPE") // Type of incident/attack, ToDo: Create Enum
    def desc = column[String]("TXT") // Syslog line of incident
    def * = (id, ts, sip, ll, dip, dport, evil, desc)
  }

  class Scans(tag: Tag) extends Table[(Int, Timestamp, Timestamp)](tag, "SCANS") {
    def ip = column[Int]("IP", O.PrimaryKey)
    def start = column[Timestamp]("START")
    def stop = column[Timestamp]("STOP")
    //def traceroute
    //def os
    //def ports/results
    def * = (ip, start, stop)
  }

  class Whois(tag: Tag) extends Table[(Int, Timestamp, Timestamp, String)](tag, "WHOIS") {
    def ip = column[Int]("IP", O.PrimaryKey)
    def start = column[Timestamp]("START")
    def stop = column[Timestamp]("STOP")
    def who = column[String]("WHO")
    def * = (ip, start, stop, who)
  }

  class Rules(tag: Tag) extends Table[(Int, String, Int, Int, Int, Boolean)](tag, "RULES") {
    def id = column[Int]("ID", O.PrimaryKey)
    def pattern = column[String]("PATTERN")
    def reps = column[Int]("REPS")
    def findtime = column[Int]("FINDTIME")
    def bantime = column[Int]("BANTIME")
    //def started = column[Timestamp]("STARTED")
    def active = column[Boolean]("ACTIVE")
    // tcp/udp
    // ignoreip
    // target matching
    def * = (id, pattern, reps, findtime, bantime, active)
  }

  /*
 External programs:
  - nmap
  - traceroute
  - [j]whois
  - host (or could use DNS resolver libs)
  * The programs need to exist, or otherwise this node can't be a scanner node
  * If 'which' does not exist, then progs are called w/out absolute path...
  * ...later it might be goodto add some type of anti-trojan verification of the progs
 */

  object main extends App {
    import scala.concurrent.duration._

    val logger = LoggerFactory.getLogger("main")

    print("args: ")
    args foreach print _
    println()

    val db = Database.forURL("jdbc:h2:~/scanner.h2;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    val sess = db.createSession()

    val attacks = TableQuery[Attacks]
    val scans = TableQuery[Scans]
    val whois = TableQuery[Whois]
    val rules = TableQuery[Rules]

//    Future.sequence(schema.create).onComplete()
    // schema.create
    Await.result(db.run(DBIOAction.seq((attacks.schema ++ scans.schema ++ whois.schema ++ rules.schema).create)), 30 seconds)
    // Jul 17 21:21:19 srv2v sshd[11066]: Received disconnect from 116.31.116.37: 11:  [preauth]
    rules.insertOrUpdate(1, "^sshd.*Received disconnect from $ip: .*\\[preauth\\]", 1, 0, 3600, true)
    // Jul 17 21:30:10 srv2v dovecot: pop3-login: Disconnected (auth failed, 1 attempts): user=<device@dr-kalai.com>, method=PLAIN, rip=158.69.103.43, lip=162.206.51.1
    rules.insertOrUpdate(2, "^dovecot: pop3-login: Disconnected.*rip=$ip, lip=", 2, 10, 3600, true)
    // Jul 17 21:41:07 srv2v sm-mta[11778]: v6I4f33V011778: mail.actus-ilw.co.uk [92.42.121.202] (may be forged) did not issue MAIL/EXPN/VRFY/ETRN during connection to MTA
    rules.insertOrUpdate(3, "^sm-mta\\[.*\\[$ip\\].* did not issue MAIL/EXPN/VRFY/ETRN during connection to MTA", 1, 0, 3600, true)

    val system = ActorSystem("scanner")

    val nmapprog = findprog("nmap")
    val tracerouteprog = findprog("traceroute")
    val whoisprog = findprog("whois")
    val iptablesprog = findprog("iptables")

    println(nmapprog, tracerouteprog, whoisprog, iptablesprog)

    Ruler
    SyslogReceiver

    //val r = Seq(nmap, "-A", "192.168.254.5").!!
    //ruprintln(r)

    /*
    println(InetAddress.getByName("192.168.254.5").isSiteLocalAddress)
    println(InetAddress.getByName("162.206.51.1").isSiteLocalAddress)
    println(InetAddress.getByName("10.0.0.0").isSiteLocalAddress)
    */

    def findprog(prog: String): String = Seq("which", prog).!!.trim

    val running = true
    while (running)
      Thread.sleep(1000000)

    db.close()
  }

}