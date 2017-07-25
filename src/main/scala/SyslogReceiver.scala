import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import org.slf4j.LoggerFactory
import akka.io.{IO, Udp}

package syslog {

  import akka.actor.{Props, ReceiveTimeout}
  import akka.util.ByteString

  /**
    * Created by totala on 6/27/17.
    */

  object SyslogReceiver {
    import main.main._

    val logger = LoggerFactory.getLogger(classOf[SyslogReceiver])
    val syslogreceiverref = system.actorOf(Props[SyslogReceiver])
  }

//        IO(Udp) ! Udp.Bind(self, inetSockAddress, List(new ReuseAddress())) // ToDo

  class SyslogReceiver extends Actor {

    import context.system
    import SyslogReceiver._
    import ruler.Ruler._

    IO(Udp) ! Udp.Bind(self, new InetSocketAddress("0.0.0.0", 1514))

    def receive = {
      case Udp.Bound(local) =>
        logger.info(s"SyslogReceiver: bound $local")
        context.become(ready(sender()))
    }

    def ready(socket: ActorRef): Receive = {
      case Udp.Received(data, remote) =>
        logger.debug(s"SyslogReceiver/Received: $data")
        rulerref ! data
      case Udp.Unbind =>
        logger.info(s"SyslogReceiver/Unbind")
        socket ! Udp.Unbind
      case Udp.Unbound =>
        logger.info(s"SyslogReceiver/Unbound")
        context.stop(self)
    }
  }
}
