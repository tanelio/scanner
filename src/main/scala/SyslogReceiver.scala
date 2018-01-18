import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}
import org.slf4j.LoggerFactory

package syslog {

  import akka.actor.Props

  /**
    * Created by totala on 6/27/17.
    */

  object SyslogReceiver {
    import main.main._

    val logger = LoggerFactory.getLogger(classOf[SyslogReceiver])
    val syslogreceiverref = system.actorOf(Props[SyslogReceiver])
  }

  class SyslogReceiver extends Actor {

    import SyslogReceiver._
    import context.system
    import ruler.Ruler._

    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", 1514))
    // Note: using port 1514 (above 1024) instead of original 514 which would require root privileges

    def receive = {
      case Udp.Bound(local) =>
        logger.info(s"SyslogReceiver: bound $local")
        context.become(ready(sender()))
    }

    def ready(socket: ActorRef): Receive = {
      case Udp.Received(data, remote) =>
//        logger.debug(s"SyslogReceiver/Received: $data")
        rulerref ! data   // Get out of UDP receiver Actor as quickly as possible
      case Udp.Unbind =>
        logger.info(s"SyslogReceiver/Unbind")
        socket ! Udp.Unbind
      case Udp.Unbound =>
        logger.info(s"SyslogReceiver/Unbound")
        context.stop(self)
    }
  }
}

// Syslog format: https://en.wikipedia.org/wiki/Syslog
// Syslog RFC: https://tools.ietf.org/html/rfc5424
// SDF: https://docs.oracle.com/javase/6/docs/api/java/text/SimpleDateFormat.html
