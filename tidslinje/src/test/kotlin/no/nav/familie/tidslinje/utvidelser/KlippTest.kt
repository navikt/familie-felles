package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.dag
import no.nav.familie.tidslinje.somDag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Tidslinjene er på dagnivå. `fra` og `til` er dagnummer, og NULL betyr at argumentet utelates. */
class KlippTest {
    @ParameterizedTest(name = "{0} klippet til [{1}, {2}] = {3}")
    @CsvSource(
        delimiter = '|',
        nullValues = ["NULL"],
        value = [
            "12345   | 1     | 3     | -234",
            "12345   | 0     | 4     | 12345",
            // Grensene ligger utenfor tidslinjen
            "-12     | 0     | 5     | -12",
            // Bare start eller slutt
            "12345   | 2     | NULL  | --345",
            "12345   | NULL  | 1     | 12",
            // Uendelig slutt
            "1>      | 2     | 4     | --111",
            "1>      | 3     | NULL  | ---1>",
            // Uendelig start
            "<12     | NULL  | 0     | <1",
            "<12345  | 2     | 3     | --34",
            // Ingen overlapp
            "12345   | 7     | 9     | ''",
            "12345   | 3     | 1     | ''",
            // Hull i endene fjernes, Null beholdes
            "1-1     | 1     | 1     | ''",
            "1-2     | 0     | 1     | 1",
            "1n1     | 1     | 1     | -n",
        ],
    )
    fun `klipp begrenser tidslinjen til datoene`(
        tidslinje: String,
        fra: Int?,
        til: Int?,
        forventet: String,
    ) {
        val resultat =
            when {
                fra != null && til != null -> tidslinje.dag().klipp(dag(fra), dag(til))
                fra != null -> tidslinje.dag().klipp(startTidspunkt = dag(fra))
                til != null -> tidslinje.dag().klipp(sluttTidspunkt = dag(til))
                else -> tidslinje.dag().klipp()
            }
        assertEquals(forventet, resultat.somDag())
    }
}
