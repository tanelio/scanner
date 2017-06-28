import java.net.{DatagramSocket, InetSocketAddress, SocketAddress}
import java.text.SimpleDateFormat

import akka.actor.Actor

/**
  * Created by totala on 6/27/17.
  */
class SyslogReceiver extends Actor {
  private val ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  private val OLD_SYSLOG_DATE_FORMAT = new SimpleDateFormat("MMM dd HH:mm:ss")

  private val socket = new DatagramSocket
  private val server = "192.168.254.3:514"
  private[logging] val dest: SocketAddress = server.split(":", 2).toList match {
    case host :: port :: Nil => new InetSocketAddress(host, port.toInt)
    case host :: Nil => new InetSocketAddress(host, 514 /*Syslog.DEFAULT_PORT*/)
    case _ => null
  }

  override def receive: Receive = ???
}
