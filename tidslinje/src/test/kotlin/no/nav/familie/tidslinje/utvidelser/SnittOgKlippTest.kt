package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SnittOgKlippTest {
    @Test
    fun `kan ta snittet av to tidslinjer med ulik lengde`() {
        val tidslinje1 =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2022, 2, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(periodeVerdi = 1, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 2, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 15, lengde = 2, erUendelig = false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val tidslinje2 =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2022, 2, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(periodeVerdi = 1, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 2, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 15, lengde = 11, erUendelig = false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val resultat =
            tidslinje1.biFunksjonSnitt(tidslinje2) { el1, el2 ->
                if (el1 is Udefinert || el2 is Udefinert) {
                    Udefinert()
                } else {
                    Verdi(el1.verdi!! + el2.verdi!!)
                }
            }

        val fasit = mutableListOf(2, 4, 30)

        assertEquals(
            fasit,
            resultat.innhold.map { it.periodeVerdi.verdi }.toList(),
            "Kunne ikke addere to tidslinjer med ulik slutt på månedsnivå",
        )
        assertEquals(resultat.tidsEnhet, TidsEnhet.MÅNED)

        val endDate = LocalDate.of(2022, 2, 1).plusMonths(3)
        assertEquals(endDate.withDayOfMonth(endDate.lengthOfMonth()), resultat.kalkulerSluttTidspunkt())
    }

    @Test
    fun `kan klippe en tidslinje slik at dem får riktig start og sluttdato`() {
        var tidslinje =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2022, 2, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(periodeVerdi = 1, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 2, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 15, lengde = 11, erUendelig = false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        var startDato = LocalDate.of(2022, 3, 1)
        var sluttDato = LocalDate.of(2022, 4, 30)

        var fasit = mutableListOf(2, 15)

        tidslinje = tidslinje.klipp(startDato, sluttDato)

        assertEquals(fasit, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
        assertEquals(startDato, tidslinje.startsTidspunkt)
        assertEquals(sluttDato, tidslinje.kalkulerSluttTidspunkt())

        tidslinje =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2022, 2, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(periodeVerdi = 1, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 2, lengde = 1, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 15, lengde = 11, erUendelig = false),
                    ),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        startDato = LocalDate.of(2022, 2, 3)
        sluttDato = LocalDate.of(2022, 2, 6)

        fasit = mutableListOf(1)

        tidslinje = tidslinje.konverterTilDag().klipp(startDato, sluttDato)

        assertEquals(fasit, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
        assertEquals(startDato, tidslinje.startsTidspunkt)
        assertEquals(sluttDato, tidslinje.kalkulerSluttTidspunkt())
    }

    @Test
    fun `klipping av tidslinje utenfor tidslinje gjør ingenting`() {
        val tidslinje =
            Tidslinje(
                startsTidspunkt = LocalDate.of(2025, 1, 1),
                perioder = listOf(TidslinjePeriode(periodeVerdi = 1, lengde = 10, erUendelig = false)),
                tidsEnhet = TidsEnhet.MÅNED,
            )

        val startDato = LocalDate.MIN
        val sluttDato = LocalDate.MAX

        val klippetTidslinje = tidslinje.klipp(startDato, sluttDato)

        assertEquals(tidslinje, klippetTidslinje)
    }

    @Test
    fun `klipping av fom skal ikke gjøre en uendelig tidslinje endelig`() {
        val tidslinje =
            Tidslinje(
                startsTidspunkt = LocalDate.of(0, 1, 1),
                perioder =
                    listOf(
                        TidslinjePeriode(periodeVerdi = 970, lengde = 737484, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 1054, lengde = 1461, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 1083, lengde = 122, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 1310, lengde = 184, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 1510, lengde = 244, erUendelig = false),
                        TidslinjePeriode(periodeVerdi = 1766, lengde = 1000000000, erUendelig = true),
                    ),
            )
        val fom = LocalDate.of(2025, 1, 6)

        val klippet = tidslinje.klipp(startTidspunkt = fom)

        assertTrue(klippet.innhold.last().erUendelig)
    }
}
