import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import org.slf4j.LoggerFactory
//import akka.event.Logging
import akka.io.{IO, Udp}

package syslog {

  import akka.actor.Props

  /**
    * Created by totala on 6/27/17.
    */

  /*
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
*/

  object SyslogReceiver {
    val logger = LoggerFactory.getLogger(classOf[SyslogReceiver])

    val syslogref = Props[Syslog]
    val syslogreceiverref = Props[SyslogReceiver]
  }

  /*
class SyslogReceiver(val port: Int)  extends Actor {

  private val encoding = "US-ASCII"

        IO(Udp) ! Udp.Bind(self, inetSockAddress, List(new ReuseAddress())) // ToDo

  private def ready(connection: ActorRef): Receive = {
    case Udp.Received(d: ByteString, f: InetSocketAddress) =>
      try {
        val data = d.decodeString(encoding) // d.utf8String // ToDo
}
*/

  class SyslogReceiver extends Actor {

    import context.system

    import SyslogReceiver._
    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", 1514))

    def receive = {
      case Udp.Bound(local) =>
        logger.info(s"SyslogReceiver: bound $local")
        context.become(ready(sender()))
    }

    def ready(socket: ActorRef): Receive = {
      case Udp.Received(data, remote) =>
        logger.debug(s"SyslogReceiver/Received: $data")
        syslogref ! data
      case Udp.Unbind =>
        logger.info(s"SyslogReceiver/Unbind")
        socket ! Udp.Unbind
      case Udp.Unbound =>
        logger.info(s"SyslogReceiver/Unbound")
        context.stop(self)
    }
  }

  class Syslog extends Actor {
    def receive = {
      case x =>
        println(x)
    }
  }

}