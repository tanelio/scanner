import java.sql.Timestamp

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

import scala.sys.process._

package main {

  import ruler.Ruler
  import slick.jdbc.meta.MTable
  import syslog.SyslogReceiver

  import scala.concurrent.Await

  /**
    * Created by totala on 6/13/17.
    */

  // Evils: Super | Genius | Resident | Casual | Happles
  class Attacks(tag: Tag) extends Table[(Int, Timestamp, Int, Boolean, String, Int, Int, String)](tag, "ATTACKS") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def ts = column[Timestamp]("TS") // When incident occurred
    def sip = column[Int]("SIP") // SourceIP
    def ll = column[Boolean]("LL") // LinkLocal [Y|N] don't scan locals
    def dip = column[String]("DIP") // DestinationIP
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

  class Rules(tag: Tag) extends Table[(Int, String, String, Int, Int, Int, Boolean, String)](tag, "RULES") {
    def id = column[Int]("ID", O.PrimaryKey)
    def preamble = column[String]("PREAMBLE")
    def pattern = column[String]("PATTERN")
    def reps = column[Int]("REPS")
    def findtime = column[Int]("FINDTIME")
    def bantime = column[Int]("BANTIME")
    //def started = column[Timestamp]("STARTED")
    def active = column[Boolean]("ACTIVE")
    // tcp/udp
    // ignoreip
    // target matching
    def action = column[String]("ACTION")
    def * = (id, preamble, pattern, reps, findtime, bantime, active, action)
  }

  // ToDo: implement actions & firewall chains
  class Actions(tag: Tag) extends Table[(String, String)](tag, "ACTIONS") {
    def id = column[String]("ID")
    def action = column[String]("ACTION")
    def * = (id, action)
  }

  /*
 External programs:
  - nmap
  - traceroute
  - [j]whois
  - host (or could use DNS resolver libs)
  * The programs need to exist, or otherwise this node can't be a scanner node
  * If 'which' does not exist, then progs are called w/out absolute path...
  * ...later it might be good to add some type of anti-trojan verification of the progs
 */

  object main extends App {
    import scala.concurrent.duration._
    import scala.concurrent.ExecutionContext.Implicits.global

    val logger = LoggerFactory.getLogger("main")

    println("args: " + args.mkString(","))

    val db = Database.forURL("jdbc:h2:~/scanner.h2;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    val sess = db.createSession()

    val attacks = TableQuery[Attacks]
    val scans = TableQuery[Scans]
    val whois = TableQuery[Whois]
    val rules = TableQuery[Rules]
    val actions = TableQuery[Actions]

//    Future.sequence(schema.create).onComplete()
    // schema.create
    //Await.result(db.run(DBIOAction.seq((attacks.schema ++ scans.schema ++ whois.schema ++ rules.schema).create)), 30 seconds)

    println(s"creating tab;es/schema")
    val schema = attacks.schema ++ scans.schema ++ whois.schema ++ rules.schema ++ actions.schema

    val tables = List(attacks, scans, whois, rules, actions)

    val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
      val names = v.map(mt => mt.name.name)
      val createIfNotExist = tables.filter( table =>
        (!names.contains(table.baseTableRow.tableName))).map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    println(s"Creating initial data")
    val setup = DBIO.seq(

      // Jul 17 21:21:19 srv2v sshd[11066]: Received disconnect from 116.31.116.37: 11:  [preauth]
      rules += (1, "", "^sshd.+Received disconnect from $ipv4: .+\\[preauth\\]", 1, 0, 3600, true, "ssh"),
      // Jul 17 21:30:10 srv2v dovecot: pop3-login: Disconnected (auth failed, 1 attempts): user=<device@dr-kalai.com>, method=PLAIN, rip=158.69.103.43, lip=162.206.51.1
      rules += (2, "", "^dovecot: pop3-login: Disconnected.+rip=$ipv4, lip=", 2, 10, 3600, true, "dovecot"),
      // Jul 30 11:39:07 srv2v saslauthd[1771]: do_auth         : auth failure: [user=miller] [service=smtp] [realm=otala.com] [mech=pam] [reason=PAM auth error]
      // Jul 17 21:41:07 srv2v sm-mta[11778]: v6I4f33V011778: mail.actus-ilw.co.uk [92.42.121.202] (may be forged) did not issue MAIL/EXPN/VRFY/ETRN during connection to MTA
      rules += (3, "^saslauthd.+do_auth.+auth failure.+\\[user=(\\w+)\\].+",
                   "^sm-mta\\[.+\\[$ipv4\\].+did not issue MAIL/EXPN/VRFY/ETRN during connection to MTA", 1, 0, 3600, true, "sasl"),

      actions += ("ssh", "22"),
      actions += ("dovecot", "110,143"),
      actions += ("sasl", "25,465")
    )

    Await.result(db.run(setup), 30 seconds)

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