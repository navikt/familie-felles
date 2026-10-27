package no.nav.familie.tidslinje

import no.nav.familie.tidslinje.utvidelser.verdiPåTidspunkt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OppslagTest {
    @ParameterizedTest(name = "{0} i måned {1} = {2}")
    @CsvSource(
        delimiter = '|',
        nullValues = ["NULL"],
        value = [
            "1n-2>   | 0     | 1",
            "1n-2>   | 1     | NULL",
            "1n-2>   | 2     | NULL",
            "1n-2>   | 50    | 2",
            "1n-2>   | -1    | NULL",
            "<1      | -100  | 1",
            "''      | 0     | NULL",
        ],
    )
    fun `verdiPåTidspunkt gir verdien på datoen, eller null`(
        tidslinje: String,
        måned: Int,
        forventet: Int?,
    ) {
        assertEquals(forventet, tidslinje.mnd().verdiPåTidspunkt(mnd(måned)))
        assertEquals(forventet, tidslinje.mnd().verdiPåTidspunkt(mndSlutt(måned)))
    }

    @Test
    fun `inneholder sjekker om verdien finnes i tidslinjen`() {
        assertEquals(true, "1n2>".mnd().inneholder(2))
        assertEquals(false, "1n2>".mnd().inneholder(3))
    }

    @ParameterizedTest(name = "{0} er tom: {1}")
    @CsvSource(
        delimiter = '|',
        value = [
            "''      | true",
            "n       | false",
            "-1      | false",
            "1>      | false",
        ],
    )
    fun `erTom og erIkkeTom`(
        tidslinje: String,
        erTom: Boolean,
    ) {
        assertEquals(erTom, tidslinje.mnd().erTom())
        assertEquals(!erTom, tidslinje.mnd().erIkkeTom())
    }

    @ParameterizedTest(name = "{0} og {1} overlapper: {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "11      | -1      | true",
            "1       | -1      | false",
            // Null regnes ikke som overlapp
            "1n      | -1      | false",
            "1>      | --1     | true",
            "<1      | -1>     | false",
            "''      | 1       | false",
        ],
    )
    fun `harOverlappMed og harIkkeOverlappMed sjekker om begge har verdi samtidig`(
        a: String,
        b: String,
        overlapper: Boolean,
    ) {
        assertEquals(overlapper, a.mnd().harOverlappMed(b.mnd()))
        assertEquals(!overlapper, a.mnd().harIkkeOverlappMed(b.mnd()))
    }

    @Test
    fun `kalkulerSluttTidspunkt gir siste dag i tidslinjen`() {
        assertEquals(mndSlutt(1), "12".mnd().kalkulerSluttTidspunkt())
        assertEquals(PRAKTISK_SENESTE_DAG, "1>".mnd().kalkulerSluttTidspunkt())
        assertEquals(PRAKTISK_TIDLIGSTE_DAG.minusDays(1), "".mnd().kalkulerSluttTidspunkt())
    }

    @ParameterizedTest(name = "fom {0}, tom {1} omfatter {2}: {3}")
    @CsvSource(
        nullValues = ["NULL"],
        value = [
            "2020-01-01, 2020-01-31, 2020-01-01, true",
            "2020-01-01, 2020-01-31, 2020-01-31, true",
            "2020-01-01, 2020-01-31, 2020-02-01, false",
            "2020-01-01, 2020-01-31, 2019-12-31, false",
            "NULL,       2020-01-31, 1900-01-01, true",
            "2020-01-01, NULL,       2999-01-01, true",
            "NULL,       NULL,       2020-01-01, true",
        ],
    )
    fun `omfatter sjekker om datoen ligger i perioden`(
        fom: java.time.LocalDate?,
        tom: java.time.LocalDate?,
        dato: java.time.LocalDate,
        forventet: Boolean,
    ) {
        assertEquals(forventet, Periode(1, fom, tom).omfatter(dato))
    }
}
