package no.nav.familie.felles.tokenklient.entraid

import io.mockk.mockk
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

class EntraIDRestClientFactoryTest {
    private val entraIDClient = mockk<EntraIDClient>()
    private val consumerIdInterceptor = mockk<ConsumerIdClientInterceptor>()
    private val mdcInterceptor = mockk<MdcValuesPropagatingClientInterceptor>()

    private val factory =
        EntraIDRestClientFactory(
            entraIDClient = entraIDClient,
            consumerIdClientInterceptor = consumerIdInterceptor,
            mdcValuesPropagatingClientInterceptor = mdcInterceptor,
        )

    @Test
    fun `skal opprette en RestClient for gitt target`() {
        val client = factory.lagMaskinTilMaskinRestKlient("api://min-tjeneste/.default")

        Assertions.assertNotNull(client)
    }

    @Test
    fun `skal opprette uavhengige RestClient-instanser per target`() {
        val client1 = factory.lagMaskinTilMaskinRestKlient("api://tjeneste-a/.default")
        val client2 = factory.lagMaskinTilMaskinRestKlient("api://tjeneste-b/.default")

        Assertions.assertNotNull(client1)
        Assertions.assertNotNull(client2)
        assertNotEquals(client1, client2, "Forventer at forskjellige targets gir forskjellige RestClient-instanser")
    }

    @Test
    fun `skal opprette en obo RestClient for gitt target`() {
        val client = factory.lagOboRestKlient("api://min-tjeneste/.default") { "bruker-token" }

        Assertions.assertNotNull(client)
    }

    @Test
    fun `skal opprette uavhengige obo RestClient-instanser per target`() {
        val client1 = factory.lagOboRestKlient("api://tjeneste-a/.default") { "bruker-token" }
        val client2 = factory.lagOboRestKlient("api://tjeneste-b/.default") { "bruker-token" }

        Assertions.assertNotNull(client1)
        Assertions.assertNotNull(client2)
        assertNotEquals(client1, client2, "Forventer at forskjellige targets gir forskjellige RestClient-instanser")
    }
}
