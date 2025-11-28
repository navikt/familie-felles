package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.math.BigDecimal
import java.time.LocalDate

@Tag("integration")
class NorgesBankValutakursRestKlientIntegrasjonTest {
    @Test
    fun `Test mot Norges Bank at vi får hentet valutakurs`() {
        val kursdato = LocalDate.of(2025, 3, 13)
        val config = SDMXValutakursRestKlientConfig()

        val valutakurs =
            NorgesBankValutakursRestKlient(
                restOperations = config.xmlRestTemplate(),
            ).hentValutakurs(
                frekvens = Frekvens.VIRKEDAG,
                valuta = "EUR",
                kursDato = kursdato,
            )

        assertNotNull(valutakurs)
        assertEquals(kursdato, valutakurs.kursDato)
        assertEquals("EUR", valutakurs.valuta)
        assertEquals(BigDecimal.valueOf(11.5975), valutakurs.kurs)
    }
}
