package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.PRAKTISK_TIDLIGSTE_DAG
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.tilPeriodeVerdi
import no.nav.familie.tidslinje.tomTidslinje

fun <T> Collection<Tidslinje<T>>.slåSammen(): Tidslinje<Collection<T>> {
    val minsteTidspunkt = this.minOfOrNull { it.startsTidspunkt } ?: PRAKTISK_TIDLIGSTE_DAG
    return this.fold(tomTidslinje(startsTidspunkt = minsteTidspunkt)) { sammenlagt, neste ->
        sammenlagt.biFunksjon(neste) { periodeVerdiFraSammenlagt, periodeVerdiFraNeste ->
            when (periodeVerdiFraSammenlagt) {
                is Verdi ->
                    when (periodeVerdiFraNeste) {
                        is Verdi -> Verdi(periodeVerdiFraSammenlagt.verdi + periodeVerdiFraNeste.verdi)
                        else -> periodeVerdiFraSammenlagt
                    }

                is Null ->
                    when (periodeVerdiFraNeste) {
                        is Verdi -> Verdi(listOf(periodeVerdiFraNeste.verdi))
                        else -> Null()
                    }

                is Udefinert ->
                    when (periodeVerdiFraNeste) {
                        is Verdi -> Verdi(listOf(periodeVerdiFraNeste.verdi))
                        is Null -> Null()
                        is Udefinert -> Udefinert()
                    }
            }
        }
    }
}

fun <I, R> Collection<Tidslinje<I>>.kombiner(listeKombinator: (Iterable<I>) -> R?): Tidslinje<R> =
    this.slåSammen().map {
        when (it) {
            is Verdi -> {
                val resultat = listeKombinator(it.verdi)
                if (resultat != null) Verdi(resultat) else Null()
            }

            is Null -> Null()
            is Udefinert -> Udefinert()
        }
    }

fun <T, R, RESULTAT> Tidslinje<T>.kombinerMed(
    annen: Tidslinje<R>,
    kombineringsfunksjon: (elem1: T?, elem2: R?) -> RESULTAT?,
): Tidslinje<RESULTAT> =
    this.biFunksjon(annen) { periodeverdiVenstre, periodeverdiHøyre ->
        kombineringsfunksjon(periodeverdiVenstre.verdi, periodeverdiHøyre.verdi)
            .tilPeriodeVerdi()
    }

fun <T, R, S, RESULTAT> Tidslinje<T>.kombinerMed(
    tidslinje2: Tidslinje<R>,
    tidslinje3: Tidslinje<S>,
    kombineringsfunksjon: (elem1: T?, elem2: R?, elem3: S?) -> RESULTAT?,
): Tidslinje<RESULTAT> =
    this.biFunksjon(tidslinje2, tidslinje3) { periodeVerdi1, periodeVerdi2, periodeVerdi3 ->
        kombineringsfunksjon(periodeVerdi1.verdi, periodeVerdi2.verdi, periodeVerdi3.verdi)
            .tilPeriodeVerdi()
    }
