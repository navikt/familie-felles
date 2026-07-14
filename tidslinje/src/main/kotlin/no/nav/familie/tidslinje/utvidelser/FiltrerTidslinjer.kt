package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.beskjærEtter

fun <T> Tidslinje<T>.filtrer(predicate: (T?) -> Boolean) =
    this.map {
        when (it) {
            is Verdi, is Null -> if (predicate(it.verdi)) it else Udefinert()
            is Udefinert -> Udefinert()
        }
    }

fun <T> Tidslinje<T>.filtrerIkkeNull(): Tidslinje<T> = filtrer { it != null }

fun <T> Tidslinje<T>.filtrerIkkeNull(filter: (T) -> Boolean): Tidslinje<T> = filtrer { it != null && filter(it) }

/**
 * Extension-metode for å filtrere tidslinjen mot en boolsk tidslinje
 * Resultatet får samme lengde som tidslinjen det opereres på
 * Det vil finnes perioder som tilsvarer periodene fra kilde-tidslinjen,
 * men innholdet blir null hvis den boolske tidslinjen er false
 */
fun <T> Tidslinje<T>.filtrerMed(boolskTidslinje: Tidslinje<Boolean>): Tidslinje<T> =
    this
        .kombinerMed(boolskTidslinje) { verdi, erSann ->
            when (erSann) {
                true -> verdi
                else -> null
            }
        }.beskjærEtter(this)

/**
 * Extension-metode for å filtrere innholdet i en map av tidslinjer
 */
fun <K, T> Map<K, Tidslinje<T>>.filtrerHverKunVerdi(filter: (T) -> Boolean): Map<K, Tidslinje<T>> =
    mapValues { (_, tidslinje) -> tidslinje.filtrer { if (it != null) filter(it) else false } }
