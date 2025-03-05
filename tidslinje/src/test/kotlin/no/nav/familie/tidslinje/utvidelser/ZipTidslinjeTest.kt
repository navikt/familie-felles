package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ZipTidslinjeTest {
    private val nå = LocalDate.now()

    private val tidslinje =
        listOf(
            Periode('a', nå.plusDays(1), nå.plusDays(1)),
            Periode('b', nå.plusDays(2), nå.plusDays(2)),
            Periode('c', nå.plusDays(3), nå.plusDays(3)),
        ).tilTidslinje()

    @Test
    fun testZipMedNesteTidslinjePaddingFør() {
        val zippetTidslinje = tidslinje.zipMedNeste(ZipPadding.FØR)

        assertEquals(
            zippetTidslinje.tilPerioder().map { it.verdi },
            listOf(Pair(null, 'a'), Pair('a', 'b'), Pair('b', 'c')),
        )
    }

    @Test
    fun testZipMedNesteTidslinjePaddingEtter() {
        val zippetTidslinje = tidslinje.zipMedNeste(ZipPadding.ETTER)

        assertEquals(
            zippetTidslinje.tilPerioder().map { it.verdi },
            listOf(Pair('a', 'b'), Pair('b', 'c'), Pair('c', null)),
        )
    }

    @Test
    fun testZipMedNesteTidslinjeIngenPadding() {
        val zippetTidslinje = tidslinje.zipMedNeste(ZipPadding.INGEN_PADDING)

        assertEquals(
            zippetTidslinje.tilPerioder().map { it.verdi },
            listOf(Pair('a', 'b'), Pair('b', 'c')),
        )
    }
}
