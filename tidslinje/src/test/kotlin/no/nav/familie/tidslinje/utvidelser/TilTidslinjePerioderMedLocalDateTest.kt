package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Verdi
import no.nav.familie.tidslinje.diffIDager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TilTidslinjePerioderMedLocalDateTest {
    private val førsteJanuar = LocalDate.of(2022, 1, 1)
    private val sisteDagIJanuar = LocalDate.of(2022, 1, 31)
    private val førsteFebruar = LocalDate.of(2022, 2, 1)
    private val sisteDagIFebruar = LocalDate.of(2022, 2, 28)
    private val førsteMars = LocalDate.of(2022, 3, 1)
    private val sisteDagIMars = LocalDate.of(2022, 3, 31)

    @Test
    fun `Skal gi ut liste med perioder med riktig tom og fom gitt starttidspunkt og lengde på periodene`() {
        val tidslinje =
            Tidslinje(
                førsteJanuar,
                listOf(
                    TidslinjePeriode(Verdi("a"), førsteJanuar.diffIDager(sisteDagIJanuar)),
                    TidslinjePeriode(Verdi("b"), førsteFebruar.diffIDager(sisteDagIFebruar)),
                    TidslinjePeriode(Verdi("c"), førsteMars.diffIDager(sisteDagIMars)),
                ),
            )

        val tidslinjePerioderMedDato = tidslinje.tilTidslinjePerioderMedDato()

        Assertions.assertEquals(3, tidslinjePerioderMedDato.size)

        Assertions.assertEquals(førsteJanuar, tidslinjePerioderMedDato[0].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIJanuar, tidslinjePerioderMedDato[0].tom.tilLocalDateEllerNull())
        Assertions.assertEquals("a", tidslinjePerioderMedDato[0].periodeVerdi.verdi)

        Assertions.assertEquals(førsteFebruar, tidslinjePerioderMedDato[1].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIFebruar, tidslinjePerioderMedDato[1].tom.tilLocalDateEllerNull())
        Assertions.assertEquals("b", tidslinjePerioderMedDato[1].periodeVerdi.verdi)

        Assertions.assertEquals(førsteMars, tidslinjePerioderMedDato[2].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIMars, tidslinjePerioderMedDato[2].tom.tilLocalDateEllerNull())
        Assertions.assertEquals("c", tidslinjePerioderMedDato[2].periodeVerdi.verdi)
    }

    @Test
    fun `Skal gi ut riktige datoer etter manipulering på tidslinjene`() {
        val tidslinje1 =
            Tidslinje(
                førsteJanuar,
                listOf(
                    TidslinjePeriode(Verdi("a"), førsteJanuar.diffIDager(sisteDagIMars)),
                ),
            )

        val tidslinje2 =
            Tidslinje(
                førsteFebruar,
                listOf(
                    TidslinjePeriode(Verdi("b"), førsteFebruar.diffIDager(sisteDagIFebruar)),
                ),
            )

        val tidslinjePerioderMedDato =
            tidslinje1
                .biFunksjon(tidslinje2) { a, b ->
                    if (b is Verdi) {
                        b
                    } else {
                        a
                    }
                }.tilTidslinjePerioderMedDato()

        Assertions.assertEquals(3, tidslinjePerioderMedDato.size)

        Assertions.assertEquals(førsteJanuar, tidslinjePerioderMedDato[0].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIJanuar, tidslinjePerioderMedDato[0].tom.tilLocalDateEllerNull())
        Assertions.assertEquals("a", tidslinjePerioderMedDato[0].periodeVerdi.verdi)

        Assertions.assertEquals(førsteFebruar, tidslinjePerioderMedDato[1].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIFebruar, tidslinjePerioderMedDato[1].tom.tilLocalDateEllerNull())
        Assertions.assertEquals("b", tidslinjePerioderMedDato[1].periodeVerdi.verdi)

        Assertions.assertEquals(førsteMars, tidslinjePerioderMedDato[2].fom.tilLocalDateEllerNull())
        Assertions.assertEquals(sisteDagIMars, tidslinjePerioderMedDato[2].tom.tilLocalDateEllerNull())
        Assertions.assertEquals("a", tidslinjePerioderMedDato[2].periodeVerdi.verdi)
    }
}
