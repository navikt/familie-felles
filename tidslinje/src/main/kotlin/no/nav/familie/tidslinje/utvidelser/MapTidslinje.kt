package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi

/**
 * Extension-metode for å mappe verdiene i en tidslinje der null-verdier håndteres eksplisitt.
 * [mapper]-funksjonen kalles bare for perioder som har en (ikke-null) verdi.
 * Hvis [mapper] returnerer null, blir resultatet en periode uten verdi (Null).
 * Udefinerte perioder forblir udefinerte.
 */
fun <V, R> Tidslinje<V>.mapIkkeNull(mapper: (V) -> R?): Tidslinje<R> =
    this.map { periodeVerdi ->
        when (periodeVerdi) {
            is Verdi -> mapper(periodeVerdi.verdi)?.let { Verdi(it) } ?: Null()
            is Null -> Null()
            is Udefinert -> Udefinert()
        }
    }
