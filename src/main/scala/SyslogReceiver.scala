import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import org.slf4j.LoggerFactory
//import akka.event.Logging
import akka.io.{IO, Udp}
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
  SyslogReceiver(Syslog)
}

/*
class SyslogReceiver(val port: Int)  extends Actor {

  import SyslogReceiver._

  private val inetSockAddress: InetSocketAddress = new InetSocketAddress("0.0.0.0", port)
  private val encoding = "US-ASCII"

  logger.debug(s"SyslogReceiver: listening on UDP port $port")

  def receive = {
    case StartStream =>
      try {
        IO(Udp) ! Udp.Bind(self, inetSockAddress, List(new ReuseAddress()))
        logger.debug(s"SyslogReceiver: Started")
      } catch {
        case e: Exception => logger.error("StartStream error", e)
      }
    case Udp.Bound(local: InetSocketAddress) =>
      try {
        logger.debug(s"SyslogReceiver: UDP bound on port $port}")
        context.become(ready(sender))
      } catch {
        case e: Exception => logger.error("UDP bind error", e)
      }
  }

  private def ready(connection: ActorRef): Receive = {
    case Udp.Received(d: ByteString, f: InetSocketAddress) =>
      try {
        val data = d.decodeString(encoding) // d.utf8String
        logger.debug(s"SyslogReceiver: Received from ${f.getAddress}: $data")
      } catch {
        case t: Exception => logger.error(s"Error in UDP Received: ${t.getMessage}")
      }

    case Udp.Unbind =>
      logger.debug(s"SyslogReceiver: Unbind")
      connection ! Udp.Unbind

    case Udp.Unbound =>
      logger.debug(s"SyslogReceiver: Unbound")
      context.stop(self)
  }

}
*/

class SyslogReceiver(nextActor: ActorRef) extends Actor {
  import context.system
  IO(Udp) ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", 1514))

  def receive = {
    case Udp.Bound(local) =>
      context.become(ready(sender()))
  }

  def ready(socket: ActorRef): Receive = {
    case Udp.Received(data, remote) =>
      val processed = // parse data etc., e.g. using PipelineStage
        socket ! Udp.Send(data, remote) // example server echoes back
      nextActor ! processed
    case Udp.Unbind  => socket ! Udp.Unbind
    case Udp.Unbound => context.stop(self)
  }
}

class Syslog extends Actor {
  def receive = {

  }
}