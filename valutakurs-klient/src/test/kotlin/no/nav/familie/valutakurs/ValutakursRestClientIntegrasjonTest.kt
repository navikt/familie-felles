package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.ecb.Frequency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursRestClientIntegrasjonTest {
    @Test
    fun `Test mot ECB at vi får hentet valutakurs`() {
        val kursdato = LocalDate.of(2025, 3, 13)
        val config = SDMXValutakursRestKlientConfig()

        val kurser =
            ECBValutakursRestKlient(
                restOperations = config.xmlRestTemplate(),
                ecbApiUrl = "https://data-api.ecb.europa.eu/service/data/EXR/",
            ).hentValutakurs(
                frequency = Frequency.Daily,
                currencies = listOf("NOK"),
                exchangeRateDate = kursdato,
            )

        assertEquals(1, kurser.size)
        assertEquals(kursdato, kurser.first().kursDato)
        assertEquals("NOK", kurser.first().valuta)
        assertEquals(BigDecimal.valueOf(11.5975), kurser.first().kurs)
    }
}
