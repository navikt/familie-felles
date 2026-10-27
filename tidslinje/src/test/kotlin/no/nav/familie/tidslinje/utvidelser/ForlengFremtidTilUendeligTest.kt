package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Tidslinjene er på månedsnivå, og `tidspunkt` er første dag i måneden med det nummeret. */
class ForlengFremtidTilUendeligTest {
    @ParameterizedTest(name = "{0} fra {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            // Siste periode før tidspunktet blir uendelig, og alt fra tidspunktet fjernes
            "123     | 1     | 1>",
            "1-2     | 1     | 1>",
            // Null og hull fjernes
            "12n3    | 2     | 12>",
            // Siste periode slutter i måneden for tidspunktet, og forblir endelig
            "11      | 1     | 11",
            // Tidslinjen slutter før tidspunktet, og forblir uendret
            "123     | 5     | 123",
            "1n      | 1     | 1",
            "1>      | 1     | 1>",
            // Ingen perioder før tidspunktet
            "-1      | 0     | ''",
            "n1      | 0     | ''",
        ],
    )
    fun `forlengFremtidTilUendelig gjør siste periode før tidspunktet uendelig`(
        tidslinje: String,
        tidspunkt: Int,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().forlengFremtidTilUendelig(mnd(tidspunkt)).somMnd())
    }
}
