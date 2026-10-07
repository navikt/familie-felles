package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class KombinerUtenNullMedTest {
    /** Summerer, men gir null når summen er 4, slik at det blir synlig når kombinatoren returnerer null. */
    private fun sum(vararg verdier: Int): Int? = verdier.sum().takeIf { it != 4 }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1     | 2     | 3",
            // Null der bare én har verdi
            "11    | -2    | n3",
            "1-1   | 222   | 3n3",
            "1n    | 22    | 3n",
            // Ingen overlapp gir bare Null
            "1     | --2   | nnn",
            // Kombinatoren returnerer null
            "2     | 2     | n",
            // Uendelig start og slutt
            "<11   | -2    | <n3",
            "1>    | 2     | 3n>",
            "1>    | 2>    | 3>",
        ],
    )
    fun `kombinerUtenNullMed kombinerer bare der begge har verdi`(
        a: String,
        b: String,
        forventet: String,
    ) {
        assertEquals(forventet, a.mnd().kombinerUtenNullMed(b.mnd()) { x, y -> sum(x, y) }.somMnd())
    }

    @Test
    fun `kombinerKunVerdiMed kombinerer hver tidslinje i map med samme tidslinje`() {
        val resultat = mapOf("a" to "1".mnd(), "b" to "-1".mnd()).kombinerKunVerdiMed("22".mnd()) { x, y -> sum(x, y) }

        assertEquals(mapOf("a" to "3n", "b" to "n3"), resultat.mapValues { it.value.somMnd() })
    }

    @Test
    fun `kombinerKunVerdiMed på tom map gir tom map`() {
        assertEquals(
            emptyMap<String, Any>(),
            emptyMap<String, Tidslinje<Int>>().kombinerKunVerdiMed("1".mnd()) { x, y -> sum(x, y) },
        )
    }

    @ParameterizedTest(name = "{0} + {1} + {2} = {3}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1     | 1     | 1     | 3",
            // Null der ikke alle tre har verdi
            "1     | -1    | --1   | nnn",
            "1n1   | 111   | 111   | 3n3",
            // Kombinatoren returnerer null
            "1     | 1     | 2     | n",
            "1>    | 1>    | -1>   | n3>",
        ],
    )
    fun `kombinerKunVerdiMed med tre tidslinjer kombinerer bare der alle har verdi`(
        a: String,
        b: String,
        c: String,
        forventet: String,
    ) {
        assertEquals(forventet, a.mnd().kombinerKunVerdiMed(b.mnd(), c.mnd()) { x, y, z -> sum(x, y, z) }.somMnd())
    }
}
