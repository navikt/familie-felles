package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.TidslinjePeriodeMedDato
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KombinerTidslinjerTest {
    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val førsteFebruar = LocalDate.of(2022, 2, 1)
    private val sisteDagIFebruar = LocalDate.of(2022, 2, 28)
    private val førsteMars = LocalDate.of(2022, 3, 1)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)
    private val førsteApril = LocalDate.of(2022, 4, 1)
    private val sisteDagIApril = LocalDate.of(2022, 4, 30)
    private val førsteMai = LocalDate.of(2022, 5, 1)
    private val sisteDagIMai = LocalDate.of(2022, 5, 31)

    @Test
    fun `kombinerTidslinjer - skal kunne kombinere liste av tidslinjer til én tilslinje med lister som verdier`() {
        val tidslinjeA = listOf(TidslinjePeriodeMedDato("a", førsteJanuar, sisteDagIJanuar)).tilTidslinje()
        val tidslinjeB = listOf(TidslinjePeriodeMedDato("b", førsteJanuar, sisteDagIFebruar)).tilTidslinje()
        val tidslinjeC = listOf(TidslinjePeriodeMedDato("c", førsteJanuar, sisteDagIMars)).tilTidslinje()
        val nullTidlisline =
            listOf(
                TidslinjePeriodeMedDato<String>(
                    Null(),
                    TidslinjePeriodeMedDato.Dato(førsteMai),
                    TidslinjePeriodeMedDato.Dato(sisteDagIMai),
                ),
            ).tilTidslinje()

        val kombinerteTidslinjerPerioder =
            listOf(tidslinjeA, tidslinjeB, tidslinjeC, nullTidlisline).slåSammen().tilTidslinjePerioderMedDato()

        Assertions.assertEquals(5, kombinerteTidslinjerPerioder.size)

        Assertions.assertEquals(førsteJanuar, kombinerteTidslinjerPerioder[0].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIJanuar, kombinerteTidslinjerPerioder[0].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(listOf("a", "b", "c"), kombinerteTidslinjerPerioder[0].periodeVerdi.verdi)

        Assertions.assertEquals(førsteFebruar, kombinerteTidslinjerPerioder[1].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIFebruar, kombinerteTidslinjerPerioder[1].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(listOf("b", "c"), kombinerteTidslinjerPerioder[1].periodeVerdi.verdi)

        Assertions.assertEquals(førsteMars, kombinerteTidslinjerPerioder[2].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIMars, kombinerteTidslinjerPerioder[2].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(listOf("c"), kombinerteTidslinjerPerioder[2].periodeVerdi.verdi)

        Assertions.assertEquals(førsteApril, kombinerteTidslinjerPerioder[3].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIApril, kombinerteTidslinjerPerioder[3].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Udefinert::class.java, kombinerteTidslinjerPerioder[3].periodeVerdi::class.java)

        Assertions.assertEquals(førsteMai, kombinerteTidslinjerPerioder[4].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIMai, kombinerteTidslinjerPerioder[4].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Null::class.java, kombinerteTidslinjerPerioder[4].periodeVerdi::class.java)
    }
}
