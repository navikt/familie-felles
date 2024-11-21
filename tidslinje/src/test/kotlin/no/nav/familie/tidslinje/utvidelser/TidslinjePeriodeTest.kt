package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TidslinjePeriodeTest {
    @Test
    fun `to TidslinjePerioder kan summeres og substraheres`() {
        val p1 = TidslinjePeriode(1, 2, false)
        val p2 = TidslinjePeriode(3, 2, false)

        var p3 = p1.biFunksjon(p2, 2, false) { el1, el2 -> Verdi(el1.verdi!! + el2.verdi!!) }

        assertNotNull(p3.periodeVerdi.verdi)
        Assertions.assertEquals(4, p3.periodeVerdi.verdi)

        p3 = p1.biFunksjon(p2, 2, false) { el1, el2 -> Verdi(el1.verdi!! - el2.verdi!!) }

        Assertions.assertEquals(-2, p3.periodeVerdi.verdi)
    }

    @Test
    fun `en TidslinjePeriode må ha lengde større enn null`() {
        assertThrows<java.lang.IllegalArgumentException> {
            TidslinjePeriode(1, -1, false)
        }
        assertThrows<java.lang.IllegalArgumentException> {
            TidslinjePeriode(1, 0, false)
        }
    }
}
