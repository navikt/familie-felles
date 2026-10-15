package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TrimTest {
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "nn1n    | --1",
            "1n1     | 1n1",
            "n1n>    | -1",
            "<n1     | -1",
            "nn      | ''",
            "''      | ''",
        ],
    )
    fun `trim fjerner Null i begge ender og flytter starten`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().trim(Null()).somMnd())
    }

    @Test
    fun `trimVenstre og trimHøyre fjerner bare i den ene enden`() {
        assertEquals("-1n", "n1n".mnd().trimVenstre(Null()).somMnd())
        assertEquals("n1", "n1n".mnd().trimHøyre(Null()).somMnd())
    }

    @Test
    fun `trim fjerner alle de oppgitte verdiene`() {
        assertEquals("--2", "n12n1".mnd().trim(Null(), Verdi(1)).somMnd())
    }

    @Test
    fun `trim kan fjerne hull`() {
        val medHullIEnden = "1-2".mnd().filtrer { it != 2 }

        assertEquals("1--", medHullIEnden.somMnd())
        assertEquals("1", medHullIEnden.trim(Udefinert()).somMnd())
    }
}
