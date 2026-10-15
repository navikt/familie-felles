package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.PeriodeVerdi
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Testene viser hvilken [PeriodeVerdi] biFunksjon sender inn for hver tidslinje:
 * `u` for Udefinert, `n` for Null og sifferet for en verdi.
 */
class BiFunksjonTest {
    private fun vis(verdi: PeriodeVerdi<Int>): PeriodeVerdi<Char> =
        when (verdi) {
            is Udefinert -> Verdi('u')
            is Null -> Verdi('n')
            is Verdi -> Verdi('0' + verdi.verdi)
        }

    @ParameterizedTest(name = "{0}, {1}: venstre ser {2}, høyre ser {3}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1     | 2     | 1     | 2",
            // Udefinert utenfor hver tidslinje
            "1     | --2   | 1uu   | uu2",
            "-1    | 1     | u1    | 1u",
            // Null og hull sendes videre som de er
            "1n    | -2    | 1n    | u2",
            "1-1   | 222   | 1u1   | 222",
            // Uendelig start og slutt
            "<1    | -2    | <1u   | <u2",
            "1>    | --2   | 1>    | uu2u>",
            "<1>   | -2>   | <1>   | <u2>",
        ],
    )
    fun `biFunksjon fyller med Udefinert utenfor hver tidslinje`(
        a: String,
        b: String,
        venstre: String,
        høyre: String,
    ) {
        assertEquals(venstre, a.mnd().biFunksjon(b.mnd()) { x, _ -> vis(x) }.somMnd())
        assertEquals(høyre, a.mnd().biFunksjon(b.mnd()) { _, y -> vis(y) }.somMnd())
    }

    @ParameterizedTest(name = "{0}, {1}, {2}: ser {3}, {4}, {5}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1     | -2    | --3   | 1uu   | u2u   | uu3",
            "1n    | 2     | 3>    | 1nu>  | 2u>   | 3>",
        ],
    )
    fun `biFunksjon med tre tidslinjer fyller med Udefinert utenfor hver tidslinje`(
        a: String,
        b: String,
        c: String,
        første: String,
        andre: String,
        tredje: String,
    ) {
        assertEquals(første, a.mnd().biFunksjon(b.mnd(), c.mnd()) { x, _, _ -> vis(x) }.somMnd())
        assertEquals(andre, a.mnd().biFunksjon(b.mnd(), c.mnd()) { _, y, _ -> vis(y) }.somMnd())
        assertEquals(tredje, a.mnd().biFunksjon(b.mnd(), c.mnd()) { _, _, z -> vis(z) }.somMnd())
    }

    @Test
    fun `biFunksjon kan returnere Udefinert og Null`() {
        val resultat =
            "123".mnd().biFunksjon("123".mnd()) { x, _ ->
                when (x.verdi) {
                    1 -> Udefinert()
                    2 -> Null()
                    else -> x
                }
            }

        assertEquals("-n3", resultat.somMnd())
    }
}
