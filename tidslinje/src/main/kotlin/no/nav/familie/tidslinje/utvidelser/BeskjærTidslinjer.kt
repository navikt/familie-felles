package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.tomTidslinje
import java.time.LocalDate
import java.time.YearMonth

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje etter til-og-med fra en annen tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra [this]s startTidspunkt og til [tidslinje]s sluttTidspunkt
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Hvis ny og eksisterende grenseverdi begge er uendelige, vil den nye benyttes
 * Beskjæring mot tom tidslinje vil gi tom tidslinje
 */
fun <I> Tidslinje<I>.beskjærTilOgMedEtter(tidslinje: Tidslinje<*>): Tidslinje<I> =
    when {
        tidslinje.erTom() -> tomTidslinje()
        else -> klipp(sluttTidspunkt = tidslinje.kalkulerSluttTidspunkt())
    }

/**
 * Extension-metode for å beskjære (forkorte) en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra innsendt [fraOgMed] og til [tilOgMed]
 * Perioder som ligger helt utenfor grensene vil forsvinne.
 * Perioden i hver ende som ligger delvis innenfor, vil forkortes.
 * Uendelige endepunkter vil beskjæres til endelig
 */
fun <V> Tidslinje<V>.beskjær(
    fraOgMed: LocalDate,
    tilOgMed: LocalDate,
): Tidslinje<V> =
    when {
        erTom() -> tomTidslinje()
        else -> klipp(startTidspunkt = fraOgMed, sluttTidspunkt = tilOgMed)
    }

/**
 * Extension-metode for å beskjære fom dato på en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra innsendt [fraOgMed] og til eksisterende tilOgMed
 */
fun <V> Tidslinje<V>.beskjærFraOgMed(fraOgMed: LocalDate): Tidslinje<V> =
    when {
        erTom() -> tomTidslinje()
        else -> klipp(startTidspunkt = fraOgMed)
    }

/**
 * Extension-metode for å beskjære tom dato på en tidslinje
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra eksisterende fraOgMed og til innsendt [tilOgMed]
 */
fun <V> Tidslinje<V>.beskjærTilOgMed(tilOgMed: LocalDate): Tidslinje<V> =
    when {
        erTom() -> tomTidslinje()
        else -> klipp(sluttTidspunkt = tilOgMed)
    }

/**
 * Extension-metode for å beskjære tom dato på et map av tidslinjer
 * Etter beskjæringen vil tidslinjen maksimalt strekke seg fra eksisterende fraOgMed og til innsendt [tilOgMed]
 */
fun <K, V> Map<K, Tidslinje<V>>.beskjærTilOgMed(tilOgMed: LocalDate): Map<K, Tidslinje<V>> =
    this.mapValues { (_, tidslinje) -> tidslinje.beskjærTilOgMed(tilOgMed) }

/**
 * Extension-metode for å forlenge den siste perioden i en tidslinje til å bli uendelig,
 * gitt at perioden allerede strekker seg til og med [tidspunktForUendelighet] eller senere.
 */
fun <T : Any> Tidslinje<T>.forlengFremtidTilUendelig(tidspunktForUendelighet: LocalDate): Tidslinje<T> {
    val senesteTomIPerioder = this.tilPerioderIkkeNull().mapNotNull { it.tom }.maxOrNull()

    return if (senesteTomIPerioder != null && senesteTomIPerioder >= tidspunktForUendelighet) {
        this
            .tilPerioderIkkeNull()
            .filter { it.fom != null && it.fom!! < tidspunktForUendelighet }
            .ifEmpty { return tomTidslinje() }
            .replaceLast { periode ->
                if (periode.tom?.let { YearMonth.from(it) } != YearMonth.from(tidspunktForUendelighet)) {
                    periode.copy(tom = null)
                } else {
                    periode
                }
            }.tilTidslinje()
    } else {
        this.tilPerioderIkkeNull().tilTidslinje()
    }
}

private fun <T> List<T>.replaceLast(replacer: (T) -> T): List<T> = this.take(this.size - 1) + replacer(this.last())
