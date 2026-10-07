package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Filtrering gjør perioder om til hull (`-`) og beholder lengden, også når hullet havner i enden. */
class FiltrerTest {
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "123     | 1-3",
            "1n2>    | 1n->",
            "2       | -",
            "''      | ''",
        ],
    )
    fun `filtrer gjør perioder som ikke oppfyller betingelsen om til hull`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().filtrer { it != 2 }.somMnd())
    }

    @Test
    fun `filtrer kaller betingelsen med null for Null`() {
        assertEquals("-2", "n2".mnd().filtrer { it != null }.somMnd())
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1n2     | 1-2",
            "n>      | ->",
        ],
    )
    fun `filtrerIkkeNull gjør Null om til hull`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().filtrerIkkeNull().somMnd())
    }

    @Test
    fun `filtrerIkkeNull med betingelse gjør Null og verdier som ikke oppfyller betingelsen om til hull`() {
        assertEquals("1--", "1n2".mnd().filtrerIkkeNull { it == 1 }.somMnd())
    }

    @ParameterizedTest(name = "{0} filtrert med {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            // 1 betyr sann og 0 usann
            "123     | 101     | 1n3",
            // Usann eller manglende verdi gir Null
            "123     | 10      | 1nn",
            "123     | 1-1     | 1n3",
            // Resultatet beskjæres til tidslinjen som filtreres
            "123     | 1011    | 1n3",
            "1>      | 10      | 1n>",
        ],
    )
    fun `filtrerMed beholder verdien der den boolske tidslinjen er sann`(
        tidslinje: String,
        boolsk: String,
        forventet: String,
    ) {
        val boolskTidslinje: Tidslinje<Boolean> = boolsk.mnd().mapIkkeNull { it == 1 }
        assertEquals(forventet, tidslinje.mnd().filtrerMed(boolskTidslinje).somMnd())
    }

    @Test
    fun `filtrerHverKunVerdi filtrerer hver tidslinje i map og gjør Null om til hull`() {
        val resultat = mapOf("a" to "12".mnd(), "b" to "n1".mnd()).filtrerHverKunVerdi { it == 1 }

        assertEquals(mapOf("a" to "1-", "b" to "-1"), resultat.mapValues { it.value.somMnd() })
    }
}
