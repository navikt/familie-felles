package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.mndSlutt
import no.nav.familie.tidslinje.mndTegn
import no.nav.familie.tidslinje.somMnd
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class KombinerMedTest {
    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val førsteFebruar = LocalDate.of(2022, 2, 1)
    private val sisteDagIFebruar = LocalDate.of(2022, 2, 28)
    private val førsteMars = LocalDate.of(2022, 3, 1)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)
    private val førsteApril = LocalDate.of(2022, 4, 1)
    private val sisteDagIApril = LocalDate.of(2022, 4, 30)

    /**
     * a = |111-|
     * b = |-2-2|
     * (a ?: 0) + (b ?: 0) = |1312|
     **/
    @Test
    fun `kombinerMed - Skal kombinere overlappende verdier på tidslinjene`() {
        val tidslinjeA = listOf(Periode(1, førsteJanuar, sisteDagIMars)).tilTidslinje()
        val tidslinjeB =
            listOf(Periode(2, førsteFebruar, sisteDagIFebruar), Periode(2, førsteApril, sisteDagIApril)).tilTidslinje()

        val perioder =
            tidslinjeA
                .kombinerMed(tidslinjeB) { verdiFraTidslinjeA, verdiFraTidslinjeB ->
                    (verdiFraTidslinjeA ?: 0) + (verdiFraTidslinjeB ?: 0)
                }.tilPerioder()

        Assertions.assertEquals(4, perioder.size)

        Assertions.assertEquals(førsteJanuar, perioder[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, perioder[0].tom)
        Assertions.assertEquals(1, perioder[0].verdi)

        Assertions.assertEquals(førsteFebruar, perioder[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, perioder[1].tom)
        Assertions.assertEquals(3, perioder[1].verdi)

        Assertions.assertEquals(førsteMars, perioder[2].fom)
        Assertions.assertEquals(sisteDagIMars, perioder[2].tom)
        Assertions.assertEquals(1, perioder[2].verdi)

        Assertions.assertEquals(førsteApril, perioder[3].fom)
        Assertions.assertEquals(sisteDagIApril, perioder[3].tom)
        Assertions.assertEquals(2, perioder[3].verdi)
    }

    /**
     * a = |1--|
     * b = |--2|
     * (a ?: 0) + (b ?: 0) = |102|
     **/
    @Test
    fun `kombinerMed - Skal ikke kombinere verdier som ikke overlapper`() {
        val tidslinjeA = listOf(Periode(1, førsteJanuar, sisteDagIJanuar)).tilTidslinje()
        val tidslinjeB = listOf(Periode(2, førsteMars, sisteDagIMars)).tilTidslinje()

        val perioder =
            tidslinjeA
                .kombinerMed(tidslinjeB) { verdiFraTidslinjeA, verdiFraTidslinjeB ->
                    (verdiFraTidslinjeA ?: 0) + (verdiFraTidslinjeB ?: 0)
                }.tilPerioder()

        Assertions.assertEquals(3, perioder.size)

        Assertions.assertEquals(førsteJanuar, perioder[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, perioder[0].tom)
        Assertions.assertEquals(1, perioder[0].verdi)

        Assertions.assertEquals(førsteFebruar, perioder[1].fom)
        Assertions.assertEquals(sisteDagIFebruar, perioder[1].tom)
        Assertions.assertEquals(0, perioder[1].verdi)

        Assertions.assertEquals(førsteMars, perioder[2].fom)
        Assertions.assertEquals(sisteDagIMars, perioder[2].tom)
        Assertions.assertEquals(2, perioder[2].verdi)
    }

    /** Summerer, men gir null når begge mangler verdi, slik at Null og hull blir synlige som `n`. */
    private fun sum(
        a: Int?,
        b: Int?,
    ): Int? = if (a == null && b == null) null else (a ?: 0) + (b ?: 0)

    private fun sum(
        a: Int?,
        b: Int?,
        c: Int?,
    ): Int? = if (a == null && b == null && c == null) null else (a ?: 0) + (b ?: 0) + (c ?: 0)

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            // Samme periode
            "1     | 2     | 3",
            // Like summer i naboperioder slås sammen
            "12    | 21    | 33",
            // Delvis overlapp
            "11    | -2    | 13",
            // Ingen overlapp: hullet mellom blir Null
            "1     | --2   | 1n2",
            // Den ene ligger inne i den andre
            "111   | -2    | 131",
            // Hull i den ene
            "1-1   | 2     | 3n1",
            // Den andre starter først
            "-1    | 1     | 11",
            // Null regnes som manglende verdi
            "n     | 2     | 2",
            "nn    | n     | nn",
            // Uendelig start
            "<11   | -2    | <13",
            "<1    | -2    | <12",
            "-1>   | <2    | <21>",
            // Uendelig slutt
            "1>    | 2     | 31>",
            "1>    | -2>   | 13>",
            "1>    | 2>    | 3>",
            "1>    | --2   | 1131>",
            // Uendelig i begge ender
            "<1>   | -2    | <131>",
        ],
    )
    fun `kombinerMed kombinerer hver måned fra tidligste start til seneste slutt`(
        a: String,
        b: String,
        forventet: String,
    ) {
        Assertions.assertEquals(forventet, a.mnd().kombinerMed(b.mnd(), ::sum).somMnd())
    }

    @Test
    fun `kombinerMed med ulike typer`() {
        val resultat = "12".mnd().kombinerMed("AB".mndTegn()) { tall, bokstav -> "$bokstav$tall" }

        Assertions.assertEquals(listOf(Periode("A1", mnd(0), mndSlutt(0)), Periode("B2", mnd(1), mndSlutt(1))), resultat.tilPerioder())
    }

    @Test
    fun `kombinerMed på dagnivå over månedsskifte`() {
        val a = Periode(1, LocalDate.of(2020, 1, 30), LocalDate.of(2020, 2, 2)).tilTidslinje()
        val b = Periode(2, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1)).tilTidslinje()

        Assertions.assertEquals(
            listOf(
                Periode(1, LocalDate.of(2020, 1, 30), LocalDate.of(2020, 1, 31)),
                Periode(3, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 1)),
                Periode(1, LocalDate.of(2020, 2, 2), LocalDate.of(2020, 2, 2)),
            ),
            a.kombinerMed(b, ::sum).tilPerioder(),
        )
    }

    @Test
    fun `kombinerMed på resultatet av en annen kombinering`() {
        val resultat = "1>".mnd().kombinerMed("-2".mnd(), ::sum).kombinerMed("--3>".mnd(), ::sum)

        Assertions.assertEquals("134>", resultat.somMnd())
    }

    @ParameterizedTest(name = "{0} + {1} + {2} = {3}")
    @CsvSource(
        delimiter = '|',
        value = [
            "1     | 1     | 1     | 3",
            // Hver tidslinje dekker sin måned
            "1     | -1    | --1   | 111",
            // Null regnes som manglende verdi
            "1     | n     | 2     | 3",
            "nn    | n     | -n    | nn",
            // Hull mellom alle tre
            "1     | --1   | ----1 | 1n1n1",
            // Uendelig start og slutt
            "<1    | -1    | --1   | <111",
            "1>    | 1     | -1    | 221>",
            "1>    | -2>   | --3>  | 136>",
        ],
    )
    fun `kombinerMed med tre tidslinjer`(
        a: String,
        b: String,
        c: String,
        forventet: String,
    ) {
        Assertions.assertEquals(forventet, a.mnd().kombinerMed(b.mnd(), c.mnd(), ::sum).somMnd())
    }

    @Test
    fun `kombinerMed med tre tidslinjer av ulike typer`() {
        val resultat = "1".mnd().kombinerMed("A".mndTegn(), "1".mnd().mapVerdi { it == 1 }) { tall, bokstav, sann -> "$tall$bokstav$sann" }

        Assertions.assertEquals(listOf(Periode("1Atrue", mnd(0), mndSlutt(0))), resultat.tilPerioder())
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource(
        delimiter = '|',
        nullValues = ["NULL"],
        value = [
            "12    | NULL  | 12",
            "12    | -2    | 14",
            "1>    | 2     | 31>",
        ],
    )
    fun `kombinerMedNullable gir tidslinjen uendret når den andre er null`(
        a: String,
        b: String?,
        forventet: String,
    ) {
        Assertions.assertEquals(forventet, a.mnd().kombinerMedNullable(b?.mnd(), ::sum).somMnd())
    }
}
