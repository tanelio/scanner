import akka.actor.Actor

/**
  * Created by totala on 8/22/17.
  */
package action {

  import akka.actor.ActorRef
  import main.main.{db, actions}

  import scala.collection.mutable

  object Action {
    val actionmap = mutable.HashMap.empty[String, ActorRef)
    db.run(actions.result).map(_.foreach {
      case (id, action) =>
    }

  }

  class Action extends Actor {

  }

}