package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi

fun <T> Tidslinje<T>.filtrer(predicate: (T?) -> Boolean) =
    this.map {
        when (it) {
            is Verdi, is Null -> if (predicate(it.verdi)) it else Udefinert()
            is Udefinert -> Udefinert()
        }
    }

fun <T> Tidslinje<T>.filtrerIkkeNull(): Tidslinje<T> = filtrer { it != null }
