package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Hver rad har en kommaseparert liste av tidslinjer. Hull (`-`) forblir hull når ingen av tidslinjene har
 * Null eller verdi der, mens Null bare gir Null når ingen av tidslinjene har verdi.
 */
class KombinerTest {
    private fun String.tidslinjer(): List<Tidslinje<Int>> = if (isEmpty()) emptyList() else split(",").map { it.mnd() }

    /** Summerer, men gir null når summen er 4, slik at det blir synlig når kombinatoren returnerer null. */
    private fun sum(verdier: Iterable<Int>): Int? = verdier.sum().takeIf { it != 4 }

    @ParameterizedTest(name = "[{0}] = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            // Ingen tidslinjer
            "''          | ''",
            "1           | 1",
            "1-1         | 1-1",
            "1,2         | 3",
            "1,1,1       | 3",
            "1,-2        | 12",
            "1,-1,--1    | 111",
            // Hull der ingen av tidslinjene har noe
            "1,--2       | 1-2",
            "n-1,--1     | n-2",
            // Null regnes ikke med når andre har verdi
            "n,1         | 1",
            "n           | n",
            "1n,-n       | 1n",
            // Kombinatoren returnerer null
            "2,2         | n",
            // Uendelig start og slutt
            "<1,-1       | <11",
            "1>,2        | 31>",
            "1>,-2>      | 13>",
        ],
    )
    fun `kombiner, kombinerUtenNull og kombinerUtenNullOgIkkeTom kombinerer verdiene der minst én har verdi`(
        tidslinjer: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinjer.tidslinjer().kombiner(::sum).somMnd(), "kombiner")
        assertEquals(forventet, tidslinjer.tidslinjer().kombinerUtenNull(::sum).somMnd(), "kombinerUtenNull")
        assertEquals(forventet, tidslinjer.tidslinjer().kombinerUtenNullOgIkkeTom(::sum).somMnd(), "kombinerUtenNullOgIkkeTom")
    }

    @ParameterizedTest(name = "[{0}] = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "''          | ''",
            "1,2         | 3",
            "1,--2       | 1-2",
            "n,1         | 1",
            "n           | n",
            "1>,2        | 31>",
        ],
    )
    fun `kombiner og slåSammen samler verdiene i en liste`(
        tidslinjer: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinjer.tidslinjer().kombiner().somMnd { '0' + it.sum() }, "kombiner")
        assertEquals(forventet, tidslinjer.tidslinjer().slåSammen().somMnd { '0' + it.sum() }, "slåSammen")
    }
}
