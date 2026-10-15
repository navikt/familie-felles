package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.dag
import no.nav.familie.tidslinje.somDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Tidslinjene er på dagnivå, og `fra` og `til` er dagnummer. */
class BeskjærTest {
    @ParameterizedTest(name = "{0} beskåret til [{1}, {2}] = {3}")
    @CsvSource(
        delimiter = '|',
        value = [
            "12345   | 1     | 3     | -234",
            "1>      | 2     | 4     | --111",
            "<123    | 1     | 2     | -23",
            "12      | 5     | 6     | ''",
            "''      | 0     | 1     | ''",
        ],
    )
    fun `beskjær begrenser tidslinjen til datoene`(
        tidslinje: String,
        fra: Int,
        til: Int,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.dag().beskjær(dag(fra), dag(til)).somDag())
    }

    @ParameterizedTest(name = "{0} fra og med {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "12345   | 2     | --345",
            "1>      | 2     | --1>",
            "<12     | 1     | -2",
            "12      | 0     | 12",
            "''      | 0     | ''",
        ],
    )
    fun `beskjærFraOgMed fjerner alt før datoen`(
        tidslinje: String,
        fra: Int,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.dag().beskjærFraOgMed(dag(fra)).somDag())
    }

    @ParameterizedTest(name = "{0} til og med {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "12345   | 1     | 12",
            "1>      | 2     | 111",
            "<12     | 0     | <1",
            "12      | 5     | 12",
            "''      | 0     | ''",
        ],
    )
    fun `beskjærTilOgMed fjerner alt etter datoen`(
        tidslinje: String,
        til: Int,
        forventet: String,
    ) {
        assertEquals(forventet, tidslinje.dag().beskjærTilOgMed(dag(til)).somDag())
    }

    @Test
    fun `beskjærTilOgMed beskjærer hver tidslinje i map`() {
        val resultat = mapOf("a" to "123".dag(), "b" to "1>".dag()).beskjærTilOgMed(dag(1))

        assertEquals(mapOf("a" to "12", "b" to "11"), resultat.mapValues { it.value.somDag() })
    }
}
