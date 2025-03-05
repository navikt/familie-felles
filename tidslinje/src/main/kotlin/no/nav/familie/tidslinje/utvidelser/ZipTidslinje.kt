package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje

enum class ZipPadding {
    FØR,
    ETTER,
    INGEN_PADDING,
}

fun <V> Tidslinje<V>.zipMedNeste(zipPadding: ZipPadding = ZipPadding.INGEN_PADDING): Tidslinje<Pair<V?, V?>> {
    val padding = listOf(Periode(null, null, null))

    return when (zipPadding) {
        ZipPadding.FØR -> padding + tilPerioder()
        ZipPadding.ETTER -> tilPerioder() + padding
        ZipPadding.INGEN_PADDING -> tilPerioder()
    }.zipWithNext { forrige, denne ->
        val verdi = forrige.verdi to denne.verdi
        val fom = if (zipPadding == ZipPadding.ETTER) forrige.fom else denne.fom
        val tom = if (zipPadding == ZipPadding.ETTER) forrige.tom else denne.tom
        Periode(verdi, fom, tom)
    }.tilTidslinje()
}
