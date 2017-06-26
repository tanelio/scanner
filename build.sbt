name := "scanner"

version := "1.0"

scalaVersion := "2.12.2"

// https://mvnrepository.com/artifact/com.typesafe.slick/slick_2.12
libraryDependencies ++= Seq("com.typesafe.slick" %% "slick" % "3.2.0",
                            "com.h2database" % "h2" % "1.4.194",
                            "org.slf4j" % "slf4j-nop" % "1.6.4",
                            "com.typesafe.akka" %% "akka-remote" % "2.5.3",
                            "com.typesafe.akka" %% "akka-actor" % "2.5.3",
                            "com.google.guava" % "guava" % "22.0",
                            "com.typesafe.slick" %% "slick-hikaricp" % "3.2.0")

