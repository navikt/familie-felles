package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.PeriodeVerdi
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tomTidslinje

fun <K, V, H, R> Map<K, Tidslinje<V>>.join(
    høyreTidslinjer: Map<K, Tidslinje<H>>,
    kombinator: (V?, H?) -> R?,
): Map<K, Tidslinje<R>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys.intersect(høyreTidslinjer.keys)

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, tomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, tomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

fun <K, V, H, R> Map<K, Tidslinje<V>>.leftJoin(
    høyreTidslinjer: Map<K, Tidslinje<H>>,
    kombinator: (V?, H?) -> R?,
): Map<K, Tidslinje<R>> {
    val venstreTidslinjer = this
    val venstreNøkler = venstreTidslinjer.keys

    return venstreNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, tomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, tomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

fun <K, V, H, R> Map<K, Tidslinje<V>>.outerJoin(
    høyreTidslinjer: Map<K, Tidslinje<H>>,
    kombinator: (V?, H?) -> R?,
): Map<K, Tidslinje<R>> {
    val venstreTidslinjer = this
    val alleNøkler = venstreTidslinjer.keys + høyreTidslinjer.keys

    return alleNøkler.associateWith { nøkkel ->
        val venstreTidslinje = venstreTidslinjer.getOrDefault(nøkkel, tomTidslinje())
        val høyreTidslinje = høyreTidslinjer.getOrDefault(nøkkel, tomTidslinje())

        venstreTidslinje.kombinerMed(høyreTidslinje, kombinator)
    }
}

fun <K, A, B, C, R> Map<K, Tidslinje<A>>.outerJoin(
    tidslinjer2: Map<K, Tidslinje<B>>,
    tidslinjer3: Map<K, Tidslinje<C>>,
    kombinator: (A?, B?, C?) -> R?,
): Map<K, Tidslinje<R>> {
    val tidslinjer1 = this
    val alleNøkler = tidslinjer1.keys + tidslinjer2.keys + tidslinjer3.keys

    return alleNøkler.associateWith { nøkkel ->
        val tidslinje1 = tidslinjer1.getOrDefault(nøkkel, tomTidslinje())
        val tidslinje2 = tidslinjer2.getOrDefault(nøkkel, tomTidslinje())
        val tidslinje3 = tidslinjer3.getOrDefault(nøkkel, tomTidslinje())

        tidslinje1.kombinerMed(tidslinje2, tidslinje3, kombinator)
    }
}

fun <T, R, RESULTAT> List<Tidslinje<T>>.join(
    operand: Tidslinje<R>,
    operator: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<R>) -> PeriodeVerdi<RESULTAT>,
): List<Tidslinje<RESULTAT>> = this.mapIndexed { _, tidslinjeBarn -> tidslinjeBarn.biFunksjon(operand, kombineringsfunksjon = operator) }

fun <T, R, RESULTAT> List<Tidslinje<T>>.join(
    operand: List<Tidslinje<R>>,
    operator: (elem1: PeriodeVerdi<T>, elem2: PeriodeVerdi<R>) -> PeriodeVerdi<RESULTAT>,
): List<Tidslinje<RESULTAT>> {
    if (this.size != operand.size) throw IllegalArgumentException("Listene må ha lik lengde")
    return this.mapIndexed { index, tidslinjeBarn ->
        tidslinjeBarn.biFunksjon(
            operand[index],
            kombineringsfunksjon = operator,
        )
    }
}
