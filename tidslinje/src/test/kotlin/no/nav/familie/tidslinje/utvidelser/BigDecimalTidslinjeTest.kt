package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class BigDecimalTidslinjeTest {
    private val jan = LocalDate.of(2022, 1, 1)
    private val des = LocalDate.of(2022, 12, 31)

    @Test
    fun `minus trekker fra hver tidslinje i map'en per nøkkel`() {
        val a = mapOf("a" to listOf(Periode(BigDecimal.TEN, jan, des)).tilTidslinje())
        val b = mapOf("a" to listOf(Periode(BigDecimal.ONE, jan, des)).tilTidslinje())

        val resultat = a.minus(b)

        assertEquals(listOf(BigDecimal("9")), resultat.getValue("a").tilPerioder().map { it.verdi })
    }

    @Test
    fun `sum summerer verdiene fra alle tidslinjene i map'en`() {
        val map =
            mapOf(
                "a" to listOf(Periode(BigDecimal.ONE, jan, des)).tilTidslinje(),
                "b" to listOf(Periode(BigDecimal.TEN, jan, des)).tilTidslinje(),
            )

        val resultat = map.sum()

        assertEquals(listOf(BigDecimal("11")), resultat.tilPerioder().map { it.verdi })
    }

    @Test
    fun `rundAvTilHeltall runder av til nærmeste heltall`() {
        val tidslinje = listOf(Periode(BigDecimal("1.5"), jan, des)).tilTidslinje()

        val resultat = tidslinje.rundAvTilHeltall()

        assertEquals(BigDecimal("1.5").setScale(0, RoundingMode.HALF_UP), resultat.tilPerioder().single().verdi)
    }
}
