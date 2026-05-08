package no.nav.familie.felles.texas

import io.mockk.mockk
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

class TexasRestClientFactoryTest {
    private val texasClient = mockk<TexasClient>()
    private val consumerIdInterceptor = mockk<ConsumerIdClientInterceptor>()
    private val mdcInterceptor = mockk<MdcValuesPropagatingClientInterceptor>()

    private val factory =
        TexasRestClientFactory(
            texasClient = texasClient,
            consumerIdClientInterceptor = consumerIdInterceptor,
            mdcValuesPropagatingClientInterceptor = mdcInterceptor,
        )

    @Test
    fun `skal opprette en RestClient for gitt target`() {
        val client = factory.lagMaskinRestKlient("api://min-tjeneste/.default")

        assertNotNull(client)
    }

    @Test
    fun `skal opprette uavhengige RestClient-instanser per target`() {
        val client1 = factory.lagMaskinRestKlient("api://tjeneste-a/.default")
        val client2 = factory.lagMaskinRestKlient("api://tjeneste-b/.default")

        assertNotNull(client1)
        assertNotNull(client2)
        assertNotEquals(client1, client2, "Forventer at forskjellige targets gir forskjellige RestClient-instanser")
    }
}
