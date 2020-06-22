package no.nav.familie.leader

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.net.InetAddress

class LeaderClientTest {

    @get:Rule
    val wireMockRule = WireMockRule(4040)

    @Test
    fun `Skal returnere null hvis environement variable ELECTOR_PATH ikke eksisterer`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns null
        assertNull(LeaderClient.isLeader())
    }

    @Test
    fun `Skal returnere true hvis pod er leader`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:4040"
        stubFor(get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"name\": \"${InetAddress.getLocalHost().hostName}\"}")))

        assertTrue(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere false hvis pod ikke er leader`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:4040"
        stubFor(get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("foobar")))

        assertFalse(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere null hvis response er tom`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:4040"
        stubFor(get(anyUrl())
                        .willReturn(aResponse()
                                            .withStatus(404)))

        assertNull(LeaderClient.isLeader())
    }
}
