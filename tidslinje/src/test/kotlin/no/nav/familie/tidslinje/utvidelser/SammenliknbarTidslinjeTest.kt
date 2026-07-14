package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SammenliknbarTidslinjeTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `minsteAvHver velger den minste verdien per nøkkel og tidspunkt`() {
        val a = mapOf("a" to listOf(Periode(5, jan, des)).tilTidslinje())
        val b = mapOf("a" to listOf(Periode(3, jan, des)).tilTidslinje())

        val resultat = minsteAvHver(a, b)

        assertEquals(listOf(3), resultat.getValue("a").tilPerioder().map { it.verdi })
    }
}
