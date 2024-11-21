package no.nav.familie.tidslinje

import no.nav.familie.tidslinje.utvidelser.biFunksjon
import no.nav.familie.tidslinje.utvidelser.tilTidslinjePerioderMedDato
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjePeriodeMedDatoTest {
    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val førsteFebruar = LocalDate.of(2022, 2, 1)
    private val sisteDagIFebruar = LocalDate.of(2022, 2, 28)
    private val førsteMars = LocalDate.of(2022, 3, 1)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)

    @Test
    fun `tilTidslinjePerioderMedDato - Skal beholde datoer ved manipulering på tidslinje`() {
        val tidslinjeA = listOf(TidslinjePeriodeMedDato("a", førsteJanuar, sisteDagIMars)).tilTidslinje()
        val tidslinjeB = listOf(TidslinjePeriodeMedDato("b", førsteFebruar, sisteDagIFebruar)).tilTidslinje()

        val tidslinjePerioderMedDato =
            tidslinjeA
                .biFunksjon(tidslinjeB) { a, b ->
                    if (b is Verdi) {
                        b
                    } else {
                        a
                    }
                }.tilTidslinjePerioderMedDato()

        Assertions.assertEquals(3, tidslinjePerioderMedDato.size)

        Assertions.assertEquals(førsteJanuar, tidslinjePerioderMedDato[0].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIJanuar, tidslinjePerioderMedDato[0].tom.tilLocalDateEllerNull())

        Assertions.assertEquals(førsteFebruar, tidslinjePerioderMedDato[1].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIFebruar, tidslinjePerioderMedDato[1].tom.tilLocalDateEllerNull())

        Assertions.assertEquals(førsteMars, tidslinjePerioderMedDato[2].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIMars, tidslinjePerioderMedDato[2].tom.tilLocalDateEllerNull())
    }

    @Test
    fun `tilTidslinjePerioderMedDato - Skal kunne håndtere splitt i tidslinje`() {
        val tidslinjePerioderMedDato =
            listOf(
                TidslinjePeriodeMedDato("a", førsteJanuar, sisteDagIJanuar),
                TidslinjePeriodeMedDato("c", førsteMars, sisteDagIMars),
            ).tilTidslinje().tilTidslinjePerioderMedDato()

        Assertions.assertEquals(3, tidslinjePerioderMedDato.size)

        Assertions.assertEquals(førsteJanuar, tidslinjePerioderMedDato[0].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIJanuar, tidslinjePerioderMedDato[0].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Verdi::class.java, tidslinjePerioderMedDato[0].periodeVerdi::class.java)
        Assertions.assertEquals("a", tidslinjePerioderMedDato[0].periodeVerdi.verdi)

        Assertions.assertEquals(førsteFebruar, tidslinjePerioderMedDato[1].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIFebruar, tidslinjePerioderMedDato[1].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Udefinert::class.java, tidslinjePerioderMedDato[1].periodeVerdi::class.java)
        Assertions.assertEquals(null, tidslinjePerioderMedDato[1].periodeVerdi.verdi)

        Assertions.assertEquals(førsteMars, tidslinjePerioderMedDato[2].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIMars, tidslinjePerioderMedDato[2].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Verdi::class.java, tidslinjePerioderMedDato[2].periodeVerdi::class.java)
        Assertions.assertEquals("c", tidslinjePerioderMedDato[2].periodeVerdi.verdi)
    }

    @Test
    fun `tilTidslinjePerioderMedDato - Skal kunne håndtere nullverdier i starten og slutten av tidslinje`() {
        val tidslinjePerioderMedDato =
            listOf(
                TidslinjePeriodeMedDato("a", null, sisteDagIJanuar),
                TidslinjePeriodeMedDato("b", førsteFebruar, sisteDagIFebruar),
                TidslinjePeriodeMedDato("c", førsteMars, null),
            ).tilTidslinje().tilTidslinjePerioderMedDato()

        Assertions.assertEquals(3, tidslinjePerioderMedDato.size)

        Assertions.assertEquals(null, tidslinjePerioderMedDato[0].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIJanuar, tidslinjePerioderMedDato[0].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Verdi::class.java, tidslinjePerioderMedDato[0].periodeVerdi::class.java)
        Assertions.assertEquals("a", tidslinjePerioderMedDato[0].periodeVerdi.verdi)

        Assertions.assertEquals(førsteFebruar, tidslinjePerioderMedDato[1].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIFebruar, tidslinjePerioderMedDato[1].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Verdi::class.java, tidslinjePerioderMedDato[1].periodeVerdi::class.java)
        Assertions.assertEquals("b", tidslinjePerioderMedDato[1].periodeVerdi.verdi)

        Assertions.assertEquals(førsteMars, tidslinjePerioderMedDato[2].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(null, tidslinjePerioderMedDato[2].tom.tilLocalDateEllerNull())
        Assertions.assertEquals(Verdi::class.java, tidslinjePerioderMedDato[2].periodeVerdi::class.java)
        Assertions.assertEquals("c", tidslinjePerioderMedDato[2].periodeVerdi.verdi)
    }

    @Test
    fun `tilTidslinje - Skal kaste feil dersom det er flere tom-datoer med nullverdi`() {
        val tidslinjePerioderMedDato =
            listOf(
                TidslinjePeriodeMedDato("a", null, sisteDagIJanuar),
                TidslinjePeriodeMedDato("b", førsteFebruar, null),
                TidslinjePeriodeMedDato("c", førsteMars, null),
            )

        Assertions.assertThrows(Exception::class.java) { tidslinjePerioderMedDato.tilTidslinje() }
    }

    @Test
    fun `tilTidslinje - Skal kaste feil dersom det er flere fom-datoer med nullverdi`() {
        val tidslinjePerioderMedDato =
            listOf(
                TidslinjePeriodeMedDato("a", null, sisteDagIJanuar),
                TidslinjePeriodeMedDato("b", null, sisteDagIFebruar),
                TidslinjePeriodeMedDato("c", førsteMars, null),
            )

        Assertions.assertThrows(Exception::class.java) { tidslinjePerioderMedDato.tilTidslinje() }
    }

    @Test
    fun `tilTidslinje - Skal kaste feil om det er overlapp i periodene`() {
        val tidslinjePerioderMedDato =
            listOf(
                TidslinjePeriodeMedDato("a", null, sisteDagIJanuar),
                TidslinjePeriodeMedDato("b", førsteFebruar, sisteDagIMars),
                TidslinjePeriodeMedDato("c", førsteMars, null),
            )

        Assertions.assertThrows(Exception::class.java) { tidslinjePerioderMedDato.tilTidslinje() }
    }

    @Test
    fun `tilTidslinje og tilTidslinjePerioderMedDato - Skal håndtere tom liste`() {
        val tidslinjePerioderMedDato = emptyList<TidslinjePeriodeMedDato<Any>>()

        Assertions.assertEquals(0, tidslinjePerioderMedDato.tilTidslinje().tilTidslinjePerioderMedDato().size)
    }
}
