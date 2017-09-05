# scanner
Scan attackers, map their changes and location

Distributed system that takes multiple roles (receiver, firewall, probe) clustering together, to capture and analyze internet attacks.

Based on capture of streaming (syslog, syslog-ng) data; collect attacks that fit patterns (rules); take actions (block)
via iptables firewall, but leave logging on.

Cluster the attackers based on similarities (networks, OS types, open ports (nmap)); when attack patterns change flag changes
as a potential new attack vector.

Periodically clean (de-cluster) attackers by comparing nmap fingerprint (cleaned / re-installed machines).

Technologies:
- program: Scala, FP
- clustering/distribution: Akka
- database: H2 (possibly changing portions to Cassandra later)
- utilities (dependencies): nmap, iptables, sudo, jwhois, which
- OS: Linux
- Platform: JVM 1.8, Scala 2.12
