package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.config.ValutakursRestClientConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@Tag("integration")
class ValutakursIntegrasjonTest {
    @Test
    fun `Test mot ECB at vi får hentet valutakurs`() {
        val kursdato = LocalDate.of(2025, 3, 13)
        val config = ValutakursRestClientConfig()

        val kurser =
            ValutakursRestClient(
                restOperations = config.xmlRestTemplate(),
                ecbApiUrl = "https://data-api.ecb.europa.eu/service/data/EXR/",
            ).hentValutakurs(
                frequency = Frequency.Daily,
                currencies = listOf("NOK"),
                exchangeRateDate = kursdato,
            )

        assertEquals(1, kurser.size)
        assertEquals(kursdato, kurser.first().date)
        assertEquals("NOK", kurser.first().currency)
        assertEquals(BigDecimal.valueOf(11.5975), kurser.first().exchangeRate)
    }
}
