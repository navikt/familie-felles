package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje

fun <K, V : Comparable<V>> minsteAvHver(
    aTidslinjer: Map<K, Tidslinje<V>>,
    bTidslinjer: Map<K, Tidslinje<V>>,
): Map<K, Tidslinje<V>> = aTidslinjer.joinIkkeNull(bTidslinjer) { a, b -> minOf(a, b) }
