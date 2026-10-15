package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MapTest {
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1n-2>   | 2n-3>",
            "<1      | <2",
            "''      | ''",
        ],
    )
    fun `map, mapVerdi og mapIkkeNull endrer verdiene og beholder Null, hull og lengde`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().map { if (it is Verdi) Verdi(it.verdi + 1) else it }.somMnd(), "map")
        assertEquals(forventet, tidslinje.mnd().mapVerdi { it?.plus(1) }.somMnd(), "mapVerdi")
        assertEquals(forventet, tidslinje.mnd().mapIkkeNull { it + 1 }.somMnd(), "mapIkkeNull")
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1n-2>   | n5-2>",
            "12      | n2",
        ],
    )
    fun `mapVerdi kaller funksjonen også for Null, og null fra funksjonen gir Null`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().mapVerdi { if (it == null) 5 else it.takeIf { verdi -> verdi != 1 } }.somMnd())
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1n-2>   | nn-2>",
        ],
    )
    fun `mapIkkeNull gir Null når funksjonen returnerer null`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().mapIkkeNull { it.takeIf { verdi -> verdi != 1 } }.somMnd())
    }

    @Test
    fun `map kan gjøre om hull til Null eller verdi`() {
        assertEquals("1nn2>", "1n-2>".mnd().map { if (it is Udefinert) Null() else it }.somMnd())
        assertEquals("112", "1-2".mnd().map { if (it is Udefinert) Verdi(1) else it }.somMnd())
    }
}
