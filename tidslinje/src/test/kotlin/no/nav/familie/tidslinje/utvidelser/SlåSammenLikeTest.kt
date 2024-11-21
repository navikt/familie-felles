package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SlåSammenLikeTest {
    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)
    private val førsteApril = LocalDate.of(2022, 4, 1)
    private val sisteDagIApril = LocalDate.of(2022, 4, 30)

    @Test
    fun `slåSammenLike - Skal slå sammen like perioder som ligger inntil hverandre`() {
        val tidslinje =
            listOf(Periode("a", førsteJanuar, sisteDagIMars), Periode("a", førsteApril, sisteDagIApril)).tilTidslinje()

        val tidslinjeSlåttSammen = tidslinje.slåSammenLikePerioder()
        val perioder = tidslinjeSlåttSammen.tilPerioder()

        Assertions.assertEquals(1, perioder.size)

        Assertions.assertEquals(førsteJanuar, perioder[0].fom)
        Assertions.assertEquals(sisteDagIApril, perioder[0].tom)
        Assertions.assertEquals("a", perioder[0].verdi)
    }

    @Test
    fun `slåSammenLike - Skal ikke slå sammen like perioder som ikke ligger inntil hverandre`() {
        val tidslinje =
            listOf(Periode("a", førsteJanuar, sisteDagIJanuar), Periode("a", førsteApril, sisteDagIApril)).tilTidslinje()

        val tidslinjeSlåttSammen = tidslinje.slåSammenLikePerioder()
        val perioder = tidslinjeSlåttSammen.tilPerioder().filtrerIkkeNull()

        Assertions.assertEquals(2, perioder.size)

        Assertions.assertEquals(førsteJanuar, perioder[0].fom)
        Assertions.assertEquals(sisteDagIJanuar, perioder[0].tom)
        Assertions.assertEquals("a", perioder[0].verdi)

        Assertions.assertEquals(førsteApril, perioder[1].fom)
        Assertions.assertEquals(sisteDagIApril, perioder[1].tom)
        Assertions.assertEquals("a", perioder[1].verdi)
    }

    @Test
    fun `slåSammenLike - Skal ikke slå sammen ulike perioder som ligger inntil hverandre`() {
        val tidslinje =
            listOf(Periode("a", førsteJanuar, sisteDagIMars), Periode("b", førsteApril, sisteDagIApril)).tilTidslinje()

        val tidslinjeSlåttSammen = tidslinje.slåSammenLikePerioder()
        val perioder = tidslinjeSlåttSammen.tilPerioder()

        Assertions.assertEquals(2, perioder.size)

        Assertions.assertEquals(førsteJanuar, perioder[0].fom)
        Assertions.assertEquals(sisteDagIMars, perioder[0].tom)
        Assertions.assertEquals("a", perioder[0].verdi)

        Assertions.assertEquals(førsteApril, perioder[1].fom)
        Assertions.assertEquals(sisteDagIApril, perioder[1].tom)
        Assertions.assertEquals("b", perioder[1].verdi)
    }
}
