package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.TidsEnhet
import no.nav.familie.tidslinje.Tidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TidslinjeInitTest {
    private val tidslinjeSomStreng = "tttttfftftfftftff"
    private val mapper =
        mapOf(
            't' to true,
            'f' to false,
        )

    @Test
    fun `kan lage tidslinjer fra en streng og en mapper`() {
        val tidslinje =
            Tidslinje.lagTidslinjeFraStreng(
                tidslinjeSomStreng,
                LocalDate.now(),
                mapper,
                TidsEnhet.MÅNED,
            )

        assertTrue { tidslinje.innhold.size == 10 }
        assertTrue { tidslinje.innhold[0].periodeVerdi.verdi!! }
        Assertions.assertEquals(
            listOf(true, false, true, false, true, false, true, false, true, false),
            tidslinje.innhold.map { it.periodeVerdi.verdi }.toList(),
        )
    }

    @Test
    fun `Kan slå sammen TidslinjePerioder med lik verdi når man initsialiserer en tidslinje med en streng`() {
        val tidslinje =
            Tidslinje.lagTidslinjeFraStreng(
                "ttttttttt",
                LocalDate.now(),
                mapper,
                TidsEnhet.MÅNED,
            )

        assertTrue { tidslinje.innhold.size == 1 }
        assertTrue { tidslinje.innhold[0].periodeVerdi.verdi!! }
        Assertions.assertEquals(listOf(true), tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
    }

    @Test
    fun `kan lage tidslinjer fra en streng`() {
        val tidslinje =
            Tidslinje.lagTidslinjeFraListe(
                tidslinjeSomStreng.toList(),
                LocalDate.now(),
                TidsEnhet.MÅNED,
            )

        assertTrue { tidslinje.innhold.size == 10 }
        Assertions.assertEquals(
            listOf('t', 'f', 't', 'f', 't', 'f', 't', 'f', 't', 'f'),
            tidslinje.innhold.map { it.periodeVerdi.verdi }.toList(),
        )
    }

    @Test
    fun `kaster unntak om mapperen ikke innholder en av karakterene`() {
        assertThrows<java.lang.IllegalArgumentException> {
            Tidslinje.lagTidslinjeFraStreng(
                "tttttttttdisse karakterene støttes ikke",
                LocalDate.now(),
                mapper,
                TidsEnhet.MÅNED,
            )
        }
    }
}
