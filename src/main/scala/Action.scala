import akka.actor.Actor
import akka.actor.{Actor, Props}
import main.main._


/**
  * Created by totala on 8/22/17.
  */
package action {

  import akka.actor.ActorRef
  import main.main.{db, actions}

  import scala.collection.mutable

  object Actions {
    val actionmap = mutable.HashMap.empty[String, ActorRef)
    db.run(actions.result).map(_.foreach {
      case (id, action) =>
        actionmap += (id -> system.actorOf(Props[Action]), action)
    }

  }

  class Action(val action: String) extends Actor {

  }

}