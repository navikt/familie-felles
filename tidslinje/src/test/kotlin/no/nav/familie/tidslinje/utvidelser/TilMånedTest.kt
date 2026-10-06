package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.somMnd
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tidslinjene inn er på dagnivå, og resultatet er på månedsnivå. Hver periode skrives som
 * `verdi:fom..tom` med datoer i 2020 på formen `MM-dd`, og tom fom eller tom betyr uendelig.
 */
class TilMånedTest {
    private fun dager(vararg perioder: String): Tidslinje<Int> =
        perioder
            .map { periode ->
                val (verdi, datoer) = periode.split(":")
                val (fom, tom) = datoer.split("..")
                Periode(verdi.takeIf { it != "n" }?.toInt(), fom.tilDato(), tom.tilDato())
            }.tilTidslinje() as Tidslinje<Int>

    private fun String.tilDato(): LocalDate? = takeIf { it.isNotEmpty() }?.let { LocalDate.parse("2020-$it") }

    private fun Int?.ellerNull(): Int? = this?.takeIf { it != 0 }

    @Test
    fun `tilMåned gir én verdi per måned`() {
        assertEquals("112", dager("1:01-01..02-29", "2:03-01..03-31").tilMåned { it.first() }.somMnd())
    }

    @Test
    fun `tilMåned sender én verdi per sammenhengende periode i måneden, ikke én per dag`() {
        val tidslinje = dager("1:01-01..01-15", "2:01-16..02-29")

        assertEquals("12", tidslinje.tilMåned { it.first() }.somMnd())
        assertEquals("22", tidslinje.tilMåned { it.last() }.somMnd())
        assertEquals("21", tidslinje.tilMåned { it.size }.somMnd())
    }

    @Test
    fun `tilMåned sender null for Null og hull`() {
        assertEquals("2", dager("1:01-01..01-15", "n:01-16..01-31").tilMåned { it.size.takeIf { _ -> it[1] == null } }.somMnd())
        assertEquals("3", dager("1:01-01..01-10", "2:01-20..01-31").tilMåned { it.size.takeIf { _ -> it[1] == null } }.somMnd())
    }

    @Test
    fun `tilMåned tar med hele måneden når tidslinjen starter eller slutter midt i en måned`() {
        assertEquals("11", dager("1:01-15..02-10").tilMåned { it.first() }.somMnd())
    }

    @Test
    fun `tilMåned gir Null når mapperen returnerer null`() {
        assertEquals("1n", dager("1:01-01..01-31", "2:02-01..02-29").tilMåned { it.first().takeIf { verdi -> verdi == 1 } }.somMnd())
    }

    @Test
    fun `tilMåned med uendelig start og slutt`() {
        assertEquals("1>", dager("1:01-15..").tilMåned { it.first() }.somMnd())
        assertEquals("<12", dager("1:..01-10", "2:01-11..02-29").tilMåned { it.first() }.somMnd())
    }

    @Test
    fun `tilMåned på tom tidslinje gir tom tidslinje`() {
        assertEquals("", dager().tilMåned { it.first() }.somMnd())
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull bruker siste dag i forrige måned og første dag i måneden`() {
        assertEquals(
            "-34",
            dager("1:01-01..01-31", "2:02-01..03-31")
                .tilMånedFraMånedsskifteIkkeNull {
                    forrige,
                    denne,
                    ->
                    forrige + denne
                }.somMnd(),
        )
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull ser bort fra endringer midt i måneden`() {
        assertEquals(
            "-24",
            dager("1:01-01..02-14", "2:02-15..03-31")
                .tilMånedFraMånedsskifteIkkeNull {
                    forrige,
                    denne,
                    ->
                    forrige + denne
                }.somMnd(),
        )
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull gir Null når én av dagene mangler verdi, og fjerner Null i endene`() {
        assertEquals(
            "--4",
            dager("1:01-01..01-31", "2:02-02..03-31")
                .tilMånedFraMånedsskifteIkkeNull {
                    forrige,
                    denne,
                    ->
                    forrige + denne
                }.somMnd(),
        )
        assertEquals(
            "-2n4",
            dager("1:01-01..02-29", "2:03-01..04-30")
                .tilMånedFraMånedsskifteIkkeNull { forrige, denne ->
                    (forrige + denne).takeIf {
                        it !=
                            3
                    }
                }.somMnd(),
        )
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull med uendelig slutt`() {
        assertEquals("-1>", dager("1:01-01..").tilMånedFraMånedsskifteIkkeNull { _, denne -> denne }.somMnd())
    }

    @Test
    fun `tilMånedFraMånedsskifteIkkeNull på tom tidslinje gir tom tidslinje`() {
        assertEquals("", dager().tilMånedFraMånedsskifteIkkeNull { _, denne -> denne }.somMnd())
    }

    @Test
    fun `tilMånedFraMånedsskifte tar med måneden etter at tidslinjen slutter`() {
        val tidslinje = dager("1:01-01..01-31", "2:02-01..02-29")

        assertEquals("132", tidslinje.tilMånedFraMånedsskifte { forrige, denne -> ((forrige ?: 0) + (denne ?: 0)).ellerNull() }.somMnd())
    }

    @Test
    fun `tilMånedFraMånedsskifte kaller mapperen også når én av dagene mangler verdi`() {
        val tidslinje = dager("1:01-01..03-31")

        assertEquals("111", tidslinje.tilMånedFraMånedsskifte { _, denne -> denne }.somMnd())
        assertEquals("-111", tidslinje.tilMånedFraMånedsskifte { forrige, _ -> forrige }.somMnd())
    }

    @Test
    fun `tilMånedFraMånedsskifte med uendelig start og slutt`() {
        assertEquals("1>", dager("1:01-01..").tilMånedFraMånedsskifte { _, denne -> denne }.somMnd())
        assertEquals("<12", dager("1:..01-31", "2:02-01..02-29").tilMånedFraMånedsskifte { _, denne -> denne }.somMnd())
    }

    @Test
    fun `tilMånedFraMånedsskifte på tom tidslinje gir tom tidslinje`() {
        assertEquals("", dager().tilMånedFraMånedsskifte { _, denne -> denne }.somMnd())
    }

    @Test
    fun `forlengTidslinjeMedEnMåned legger til Null fra dagen etter slutt og én måned frem`() {
        assertEquals(
            listOf(
                Periode(1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31)),
                Periode(null, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 3, 1)),
            ),
            dager("1:01-01..01-31").forlengTidslinjeMedEnMåned().tilPerioder(),
        )
    }

    @Test
    fun `forlengTidslinjeMedEnMåned endrer ikke en uendelig tidslinje`() {
        assertEquals("1>", dager("1:01-01..").forlengTidslinjeMedEnMåned().somMnd())
    }
}
