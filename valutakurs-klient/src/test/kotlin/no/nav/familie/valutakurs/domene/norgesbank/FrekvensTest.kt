package no.nav.familie.valutakurs.domene.norgesbank

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FrekvensTest {
    @Nested
    inner class FraVerdi {
        @ParameterizedTest
        @EnumSource(Frekvens::class)
        fun `skal mappe til korrekt Frekvens for gyldige verdier`(frekvens: Frekvens) {
            assertEquals(frekvens, Frekvens.fraVerdi(frekvens.verdi))
        }

        @Test
        fun `skal feile for ugyldig verdier`() {
            assertThrows<NoSuchElementException> {
                Frekvens.fraVerdi(verdi = "X")
            }
        }
    }
}
