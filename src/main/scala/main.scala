
import java.sql.Timestamp
import slick.lifted.Tag
import slick.jdbc.H2Profile.api._


/**
  * Created by totala on 6/13/17.
  */

//type Timestamp = sql.Timestamp

class Attacks(tag: Tag) extends Table[(Int, Timestamp, Int, Int, Int, Int, String)](tag, "ATTACKS") {
  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def ts = column[Timestamp]("TS")
  def sip = column[Int]("SIP")        // SourceIP
  def dip = column[Int]("DIP")        // DestinationIP
  def dport = column[Int]("DPORT")    // Destination Port
  def typ = column[Int]("TYPE")       // Type of incident/attack, ToDo: Create Enum
  def desc = column[String]("TXT")    // Syslog line of incident
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, ts, sip, dip, dport, typ, desc)
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
//abstract class TableName(val tableName: String)

object main {

  object loader extends App {
    print("args: ")
    args foreach print _
    println()

    val db = Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver="org.h2.Driver")
    val sess = db.createSession()
  }



}
