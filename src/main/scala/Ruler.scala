/**
  * Created by totala on 7/9/17.
  */
import java.sql.Timestamp

import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import main.main.{db, rules, sess}

package ruler {

  import scala.collection.mutable

  object Ruler {

    var Rules = mutable.HashMap.empty[Int, (String, Int, Int, Int, Timestamp)]

    // initialize rules from db
    db.run(rules.result).map(_.foreach {
      case (id, pattern, reps, findtime, bantime, started, active) =>
        if (active) {
          println(id, pattern, reps, findtime)
          Rules += (id -> (pattern, reps, findtime, bantime, started))
        }
    })

    /*
     * This should have worked... probably import collisions...
     *
    db.run(for (r <- rules if r.active === "Y") {
      println(r.pattern)
    })
    */
  }

}