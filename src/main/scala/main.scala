import java.net.InetAddress
import java.sql.Timestamp

import akka.actor.{ActorSystem, Props}
import org.slf4j.LoggerFactory

//import akka.event.Logging
import slick.jdbc.H2Profile.api._
import slick.lifted.Tag

import scala.sys.process._


/**
  * Created by totala on 6/13/17.
  */

class Attacks(tag: Tag) extends Table[(Int, Timestamp, Int, Char, Int, Int, Int, String)](tag, "ATTACKS") {
  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def ts = column[Timestamp]("TS")    // When incident occurred
  def sip = column[Int]("SIP")        // SourceIP
  def ll = column[Char]("LL")         // LinkLocal [Y|N] don't scan locals
  def dip = column[Int]("DIP")        // DestinationIP
  def dport = column[Int]("DPORT")    // Destination Port
  def typ = column[Int]("TYPE")       // Type of incident/attack, ToDo: Create Enum
  def desc = column[String]("TXT")    // Syslog line of incident
  def * = (id, ts, sip, ll, dip, dport, typ, desc)
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
  import SyslogReceiver._

  val logger = LoggerFactory.getLogger("main")

  print("args: ")
  args foreach print _
  println()

  val db = Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver="org.h2.Driver")
  val sess = db.createSession()
  // todo: create schema

  val system = ActorSystem("scanner")
  val syslogref = Props[Syslog]
  val syslogreceiverref = Props[new SyslogReceiver(syslogref)]

  val nmap = findprog("nmap")
  val traceroute = findprog("traceroute")
  val whois = findprog("whois")

  println(nmap, traceroute, whois)

  //val r = Seq(nmap, "-A", "192.168.254.5").!!
  //ruprintln(r)

  println(InetAddress.getByName("192.168.254.5").isSiteLocalAddress)
  println(InetAddress.getByName("162.206.51.1").isSiteLocalAddress)
  println(InetAddress.getByName("10.0.0.0").isSiteLocalAddress)

  def findprog(prog: String): String = Seq("which", prog).!!.trim

  val running = true
  while (running)
    Thread.sleep(1000000)

  db.close()
}
