package no.nav.familie.tidslinje

import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TidslinjeNotasjonTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "1",
            "123",
            "-1",
            "--12",
            "1-2",
            "1--2",
            "n",
            "1n2",
            "<1",
            "<112",
            "1>",
            "-12>",
            "<1>",
            "<12>",
            "n>",
        ],
    )
    fun `mnd og somMnd gir samme streng tilbake`(streng: String) {
        assertEquals(streng, streng.mnd().somMnd())
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "-1", "1-2", "<1n>", "1>"])
    fun `dag og somDag gir samme streng tilbake`(streng: String) {
        assertEquals(streng, streng.dag().somDag())
    }

    @Test
    fun `tom streng er tom tidslinje`() {
        assertTrue("".mnd().erTom())
        assertEquals("", tomTidslinje<Int>().somMnd())
    }

    @Test
    fun `like tegn etter hverandre slås sammen til én periode`() {
        assertEquals(listOf(Periode(1, mnd(0), mndSlutt(2))), "111".mnd().tilPerioder())
    }

    @Test
    fun `uendelig slutt normaliseres til ett tegn`() {
        assertEquals("1>", "111>".mnd().somMnd())
    }

    @Test
    fun `hvert tegn gir riktige datoer`() {
        assertEquals(
            listOf(
                Periode(1, null, mndSlutt(0)),
                Periode(null, mnd(1), mndSlutt(1)),
                Periode(2, mnd(2), null),
            ),
            "<1n2>".mnd().tilPerioder(),
        )
        assertEquals(listOf(Periode(3, dag(2), dag(3))), "--33".dag().tilPerioder())
        assertEquals(LocalDate.of(2020, 2, 29), mndSlutt(1))
        assertEquals(LocalDate.of(2020, 2, 29), dag(59))
    }

    @Test
    fun `bokstaver gir Char-verdier`() {
        assertEquals(listOf(Periode('A', mnd(0), mndSlutt(0)), Periode('B', mnd(1), mndSlutt(1))), "AB".mndTegn().tilPerioder())
        assertEquals("AB", "AB".mndTegn().somMnd())
        assertEquals("AB", "AB".dagTegn().somDag())
    }

    @Test
    fun `somMnd viser hull i tidslinjen`() {
        val tidslinje = listOf(Periode(1, mnd(0), mndSlutt(0)), Periode(2, mnd(2), mndSlutt(2))).tilTidslinje()
        assertEquals("1-2", tidslinje.somMnd())
    }

    @Test
    fun `somMnd viser boolske verdier som J og N`() {
        assertEquals("JN", listOf(Periode(true, mnd(0), mndSlutt(0)), Periode(false, mnd(1), mndSlutt(1))).tilTidslinje().somMnd())
    }

    @Test
    fun `somMnd tar imot egen tegn-funksjon`() {
        assertEquals("X", Periode("tekst", mnd(0), mndSlutt(0)).tilTidslinje().somMnd { 'X' })
    }

    @Test
    fun `somMnd feiler når perioden ikke følger månedsgrenser`() {
        assertThrows<IllegalArgumentException> { "11".dag().somMnd() }
    }

    @Test
    fun `somMnd feiler når tidslinjen starter før START`() {
        assertThrows<IllegalArgumentException> { Periode(1, mnd(-1), mndSlutt(0)).tilTidslinje().somMnd() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["1-", "<-1", "<>", "<", ">", "x", "A"])
    fun `ugyldig notasjon gir feil`(streng: String) {
        assertThrows<IllegalArgumentException> { streng.mnd() }
    }
}

private typealias LocalDate = java.time.LocalDate
