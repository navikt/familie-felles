package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SlåSammenLikePerioderTest {
    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            // Perioder som regnes som like, får verdien til den første
            "1234    | 1224",
            "12>     | 12>",
            "23>     | 2>",
            // Null og hull slås ikke sammen med verdier
            "2n3     | 2n3",
            "''      | ''",
        ],
    )
    fun `slåSammenLikePerioder slår sammen naboperioder som sammenligningsfunksjonen sier er like`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().slåSammenLikePerioder { a, b -> a != null && b != null && a / 2 == b / 2 }.somMnd())
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1n2     | 111",
            "<1n2>   | <1>",
        ],
    )
    fun `slåSammenLikePerioder kan slå sammen Null med verdier`(
        tidslinje: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.mnd().slåSammenLikePerioder { _, _ -> true }.somMnd())
    }
}
