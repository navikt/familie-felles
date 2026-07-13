package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Periode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun <V> Periode<V>.splittPerMåned(tilOgMedMåned: YearMonth): List<Periode<V>> {
    val førsteMåned = YearMonth.from(this.fom ?: PRAKTISK_TIDLIGSTE_DAG)
    val sisteMåned = setOfNotNull(this.tom?.let { YearMonth.from(it) }, tilOgMedMåned).minOrNull()!!

    return generateSequence(førsteMåned) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(sisteMåned) }
        .map {
            Periode(
                verdi = this.verdi,
                fom = it.atDay(1),
                tom = it.atEndOfMonth(),
            )
        }.toList()
}

fun Periode<*>.erMinst12Måneder(): Boolean = ChronoUnit.MONTHS.between(fom, tom ?: LocalDate.now()) >= 12

fun Periode<*>.erMinst6Måneder(): Boolean = ChronoUnit.MONTHS.between(fom, tom ?: LocalDate.now()) >= 6

fun Periode<*>.erMinst12MånederMedNullTomSomUendelig(): Boolean = tom?.let { ChronoUnit.MONTHS.between(fom, tom) >= 12 } ?: true
