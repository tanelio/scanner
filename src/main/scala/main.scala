//import java._

import slick.lifted.Tag
//import slick.model.{Column, Table}
import slick.jdbc.H2Profile.api._
//import java.sql.Timestamp
//import org.joda.time.DateTime


/**
  * Created by totala on 6/13/17.
  */

type Timestamp = java.sql.Timestamp;

class Attacks(tag: Tag) extends Table[(Int, Timestamp, String)](tag, "ATTACKS") {
  def id = column[Int]("SUP_ID", O.PrimaryKey) // This is the primary key column
  def ts = column[Timestamp]("TS")
  def sip = column[Int]("SIP")  // SourceIP
  def dip = column[Int]("DIP")  // DestinationIP
  def dport = column[Int]("DPORT")    // Destination Port
  def typ = column[Int]("TYPE")
  def desc = column[String]("TXT")
  def name = column[String]("SUP_NAME")
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (id, ts, name)
}

class Scans(tag: Tag) extends Table[()](tag, "SCANS") {
  def ip = column[Int]("IP", O.PrimaryKey)
  def start = column[Timestamp]("START")
  def stop = column[Timestamp]("STOP")
  //def traceroute
  //def os
  //def ports/results

}

class Whois(tag: Tag) extends Table[(Int, Timestamp, String)](tag, "WHOIS") {
  def ip = column[Int]("IP", O.PrimaryKey)
  def start = column[Timestamp]("START")
  def stop = column[Timestamp]("STOP")
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
