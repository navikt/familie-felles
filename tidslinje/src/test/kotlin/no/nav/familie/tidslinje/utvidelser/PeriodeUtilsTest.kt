package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.YearMonth

class PeriodeUtilsTest {
    @Test
    fun `splittPerMåned deler en periode opp i én periode per måned`() {
        val periode = Periode("a", LocalDate.of(2022, 1, 15), LocalDate.of(2022, 3, 10))

        val resultat = periode.splittPerMåned(YearMonth.of(2022, 3))

        assertEquals(3, resultat.size)
        assertEquals(LocalDate.of(2022, 1, 1), resultat[0].fom)
        assertEquals(LocalDate.of(2022, 1, 31), resultat[0].tom)
        assertEquals(LocalDate.of(2022, 3, 1), resultat[2].fom)
        assertEquals(LocalDate.of(2022, 3, 31), resultat[2].tom)
    }

    @Test
    fun `splittPerMåned begrenses av tilOgMedMåned selv om perioden strekker seg lenger`() {
        val periode = Periode("a", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))

        val resultat = periode.splittPerMåned(YearMonth.of(2022, 2))

        assertEquals(2, resultat.size)
    }

    @Test
    fun `erMinst12Måneder og erMinst6Måneder`() {
        val periode12 = Periode("a", LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
        val periode6 = Periode("a", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 7, 1))
        val periode1 = Periode("a", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1))

        assertTrue(periode12.erMinst12Måneder())
        assertTrue(periode12.erMinst6Måneder())
        assertFalse(periode6.erMinst12Måneder())
        assertTrue(periode6.erMinst6Måneder())
        assertFalse(periode1.erMinst6Måneder())
    }

    @Test
    fun `erMinst12MånederMedNullTomSomUendelig behandler null tom som uendelig`() {
        val uendelig = Periode("a", LocalDate.of(2021, 1, 1), null)
        val kort = Periode("a", LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 1))

        assertTrue(uendelig.erMinst12MånederMedNullTomSomUendelig())
        assertFalse(kort.erMinst12MånederMedNullTomSomUendelig())
    }

    @Test
    fun `splittPerMåned med tom null splitter til og med tilOgMedMåned`() {
        val resultat = Periode("a", LocalDate.of(2022, 11, 20), null).splittPerMåned(YearMonth.of(2023, 1))

        assertEquals(listOf(YearMonth.of(2022, 11), YearMonth.of(2022, 12), YearMonth.of(2023, 1)), resultat.map { YearMonth.from(it.fom) })
    }

    @Test
    fun `splittPerMåned gir tom liste når perioden starter etter tilOgMedMåned`() {
        assertEquals(emptyList<Periode<String>>(), Periode("a", LocalDate.of(2022, 3, 1), null).splittPerMåned(YearMonth.of(2022, 2)))
    }

    @ParameterizedTest(name = "{0} – {1}: 12 måneder {2}, 6 måneder {3}")
    @CsvSource(
        value = [
            "2021-01-01, 2022-01-01, true,  true",
            "2021-01-01, 2021-12-31, false, true",
            "2021-01-31, 2022-01-30, false, true",
            "2021-01-01, 2021-07-01, false, true",
            "2021-01-01, 2021-06-30, false, false",
        ],
    )
    fun `erMinst12Måneder og erMinst6Måneder regner hele måneder mellom fom og tom`(
        fom: LocalDate,
        tom: LocalDate,
        minst12: Boolean,
        minst6: Boolean,
    ) {
        val periode = Periode("a", fom, tom)

        assertEquals(minst12, periode.erMinst12Måneder(), "erMinst12Måneder")
        assertEquals(minst12, periode.erMinst12MånederMedNullTomSomUendelig(), "erMinst12MånederMedNullTomSomUendelig")
        assertEquals(minst6, periode.erMinst6Måneder(), "erMinst6Måneder")
    }
}
