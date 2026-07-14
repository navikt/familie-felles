package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MapTidslinjeTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `mapIkkeNull mapper bare perioder som har verdi`() {
        val tidslinje =
            listOf(
                Periode<Int?>(1, jan, jan.plusMonths(5)),
                Periode<Int?>(null, jan.plusMonths(5).plusDays(1), des),
            ).tilTidslinje()

        val resultat = tidslinje.mapIkkeNull { it?.times(10) }

        assertEquals(listOf(10, null), resultat.tilPerioder().map { it.verdi })
    }
}
