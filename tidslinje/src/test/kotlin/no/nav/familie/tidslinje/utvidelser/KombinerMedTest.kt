package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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
}
