package no.nav.familie.tidslinje

import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TilTidslinjeTest {
    @Test
    fun `perioder blir til tidslinje med hull mellom periodene`() {
        val tidslinje =
            listOf(
                Periode(1, mnd(0), mndSlutt(0)),
                Periode(2, mnd(2), mndSlutt(3)),
            ).tilTidslinje()

        assertEquals("1-22", tidslinje.somMnd())
    }

    @Test
    fun `perioder sorteres før tidslinjen lages`() {
        val tidslinje =
            listOf(
                Periode(2, mnd(2), mndSlutt(2)),
                Periode(1, mnd(0), mndSlutt(0)),
            ).tilTidslinje()

        assertEquals("1-2", tidslinje.somMnd())
    }

    @Test
    fun `verdien null gir Null, ikke hull`() {
        val tidslinje = listOf<Periode<Int?>>(Periode(1, mnd(0), mndSlutt(0)), Periode(null, mnd(1), mndSlutt(1))).tilTidslinje()

        assertEquals("1n", tidslinje.somMnd())
    }

    @Test
    fun `like naboperioder slås sammen`() {
        val tidslinje = listOf(Periode(1, mnd(0), mndSlutt(0)), Periode(1, mnd(1), mndSlutt(1))).tilTidslinje()

        assertEquals(listOf(Periode(1, mnd(0), mndSlutt(1))), tidslinje.tilPerioder())
    }

    @Test
    fun `fom og tom null gir uendelig start og slutt`() {
        assertEquals("<1", Periode(1, null, mndSlutt(0)).tilTidslinje().somMnd())
        assertEquals("1>", Periode(1, mnd(0), null).tilTidslinje().somMnd())
        assertEquals("<1>", Periode(1, null, null).tilTidslinje().somMnd())
    }

    @Test
    fun `perioder som starter og slutter midt i en måned`() {
        val tidslinje = listOf(Periode(1, dag(1), dag(2)), Periode(2, dag(4), dag(4))).tilTidslinje()

        assertEquals("-11-2", tidslinje.somDag())
    }

    @Test
    fun `periode på én dag`() {
        assertEquals("-1", Periode(1, dag(1), dag(1)).tilTidslinje().somDag())
    }

    @Test
    fun `periode over skuddårsdagen`() {
        val tidslinje = Periode(1, dag(58), dag(60)).tilTidslinje()

        assertEquals("1".repeat(3), tidslinje.somDag().drop(58))
        assertEquals(listOf(Periode(1, LocalDate.of(2020, 2, 28), LocalDate.of(2020, 3, 1))), tidslinje.tilPerioder())
    }

    @Test
    fun `tom liste gir tom tidslinje`() {
        assertTrue(emptyList<Periode<Int>>().tilTidslinje().erTom())
    }

    @Test
    fun `overlappende perioder gir feil`() {
        assertThrows<IllegalStateException> {
            listOf(Periode(1, mnd(0), mndSlutt(1)), Periode(2, mnd(1), mndSlutt(2))).tilTidslinje()
        }
    }

    @Test
    fun `tom null før siste periode gir feil`() {
        assertThrows<IllegalStateException> {
            listOf(Periode(1, mnd(0), null), Periode(2, mnd(2), mndSlutt(2))).tilTidslinje()
        }
    }

    @Test
    fun `fom null etter første periode gir feil`() {
        assertThrows<IllegalStateException> {
            listOf(Periode(1, null, mndSlutt(0)), Periode(2, null, mndSlutt(2))).tilTidslinje()
        }
    }

    @Test
    fun `TidslinjePeriodeMedDato i sortert rekkefølge blir til tidslinje`() {
        val tidslinje =
            listOf<TidslinjePeriodeMedDato<Int>>(
                TidslinjePeriodeMedDato(1, mnd(0), mndSlutt(0)),
                TidslinjePeriodeMedDato(null, mnd(1), mndSlutt(1)),
                TidslinjePeriodeMedDato(2, mnd(3), null),
            ).tilTidslinje()

        assertEquals("1n-2>", tidslinje.somMnd())
    }

    @Test
    fun `validerIngenOverlapp godtar perioder som ligger inntil hverandre`() {
        listOf(TidslinjePeriodeMedDato(1, mnd(1), mndSlutt(1)), TidslinjePeriodeMedDato(2, mnd(0), mndSlutt(0))).validerIngenOverlapp()
    }

    @Test
    fun `validerIngenOverlapp feiler med egen feilmelding ved overlapp`() {
        val feil =
            assertThrows<IllegalStateException> {
                listOf(TidslinjePeriodeMedDato(1, mnd(0), mndSlutt(1)), TidslinjePeriodeMedDato(2, mnd(1), null))
                    .validerIngenOverlapp("overlapp")
            }
        assertEquals("overlapp", feil.message)
    }

    @Test
    fun `tilPerioder gir én periode per verdi, med null for både Null og hull`() {
        assertEquals(
            listOf(
                Periode(1, null, mndSlutt(0)),
                Periode(null, mnd(1), mndSlutt(1)),
                Periode(null, mnd(2), mndSlutt(2)),
                Periode(2, mnd(3), null),
            ),
            "<1n-2>".mnd().tilPerioder(),
        )
    }

    @Test
    fun `tilPerioderIkkeNull fjerner Null og hull`() {
        assertEquals(
            listOf(Periode(1, mnd(0), mndSlutt(0)), Periode(2, mnd(3), mndSlutt(3))),
            "1n-2".mnd().tilPerioderIkkeNull(),
        )
    }

    @Test
    fun `tilPerioder og tilTidslinje gjør hull om til Null`() {
        assertEquals("1nn2", "1n-2".mnd().tilPerioder().tilTidslinje().somMnd())
    }

    @Test
    fun `tilPerioder på tom tidslinje gir tom liste`() {
        assertEquals(emptyList<Periode<Int?>>(), "".mnd().tilPerioder())
    }
}
