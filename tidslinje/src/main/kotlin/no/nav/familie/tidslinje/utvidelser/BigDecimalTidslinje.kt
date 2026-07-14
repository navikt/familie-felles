package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import java.math.BigDecimal
import java.math.RoundingMode

fun <K> Map<K, Tidslinje<BigDecimal>>.minus(bTidslinjer: Map<K, Tidslinje<BigDecimal>>): Map<K, Tidslinje<BigDecimal>> =
    this.join(bTidslinjer) { a, b ->
        when {
            a != null && b != null -> a - b
            else -> a
        }
    }

fun <K> Map<K, Tidslinje<BigDecimal>>.sum(): Tidslinje<BigDecimal> =
    values.kombinerUtenNullOgIkkeTom {
        it.reduce { sum, verdi -> sum.plus(verdi) }
    }

fun Tidslinje<BigDecimal>.rundAvTilHeltall(): Tidslinje<BigDecimal> = this.mapIkkeNull { it.setScale(0, RoundingMode.HALF_UP) }
