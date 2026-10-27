package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.beskjærEtter
import no.nav.familie.tidslinje.dag
import no.nav.familie.tidslinje.somDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Tidslinjene er på dagnivå. Testene bruker bare tidslinjer laget fra perioder. Når den andre tidslinjen er
 * resultatet av en kombinering og uendelig, kan resultatet i dag bli endelig med slutt langt frem i tid.
 */
class BeskjærEtterTest {
    @ParameterizedTest(name = "{0} beskåret etter {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "12345   | -11     | -23",
            "12345   | --1>    | --345",
            "1>      | -11     | -11",
            "1>      | --1>    | --1>",
            "<12     | -1      | -2",
            "<123    | <1      | <1",
            // Ingen overlapp
            "12      | ---1    | ''",
            "-12     | 1       | ''",
            // Tom tidslinje
            "12      | ''      | ''",
        ],
    )
    fun `beskjærEtter begrenser tidslinjen til start og slutt i den andre tidslinjen`(
        tidslinje: String,
        annen: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.dag().beskjærEtter(annen.dag()).somDag())
    }

    @ParameterizedTest(name = "{0} beskåret til og med {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "12345   | -11     | 123",
            "12      | 1111    | 12",
            "1>      | 11      | 11",
            "1>      | --1>    | 1>",
            "<12     | 1       | <1",
            "12345   | ''      | ''",
        ],
    )
    fun `beskjærTilOgMedEtter begrenser tidslinjen til slutten av den andre tidslinjen`(
        tidslinje: String,
        annen: String,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.dag().beskjærTilOgMedEtter(annen.dag()).somDag())
    }
}
