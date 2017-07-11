/**
  * Created by totala on 7/9/17.
  */

import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import main.main.{db, rules, sess}

package ruler {

  object Ruler {
    // initialize rules from db

    db.run(rules.result).map(_.foreach {
      case (id, pattern, reps, findtime, bantime, started, active) =>
        println(id, pattern, reps, findtime)
    })


    /*
    db.run(for (r <- rules if r.active === "Y") {
      println(r.pattern)
    })
    */
  }

}