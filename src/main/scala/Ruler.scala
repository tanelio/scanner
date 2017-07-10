/**
  * Created by totala on 7/9/17.
  */
package ruler {
  import main.main.{db, rules}

  object Ruler {
    // initialize rules from db
    db.run(rules.result).map(_.foreach {
      case (pattern, reps, findtime) =>
        println(pattern, reps, findtime)
    })
  }

}