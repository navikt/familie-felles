package no.nav.familie.tidslinje

import java.time.Duration
import java.time.LocalDate

val PRAKTISK_TIDLIGSTE_DAG = LocalDate.of(0, 1, 1)
val PRAKTISK_SENESTE_DAG = LocalDate.MAX.minusYears(1)

fun LocalDate.diffIDager(annen: LocalDate): Int =
    Duration
        // legger på én dag på sluttdatoen siden den er exlusive
        .between(this.atStartOfDay(), annen.plusDays(1).atStartOfDay())
        .toDaysPart()
        .toInt()
