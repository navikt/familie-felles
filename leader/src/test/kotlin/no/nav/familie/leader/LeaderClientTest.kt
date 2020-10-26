package no.nav.familie.leader

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress

class LeaderClientTest {

    private val wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().port(4040))

    @BeforeEach
    fun setUp() {
        wireMockServer.start()
    }

    @AfterEach
    fun tearDown() {
        wireMockServer.resetAll()
        wireMockServer.stop()
    }


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
        wireMockServer.stubFor(get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("{\"name\": \"${InetAddress.getLocalHost().hostName}\"}")))

        assertTrue(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere false hvis pod ikke er leader`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:4040"
        wireMockServer.stubFor(get(anyUrl())
                        .willReturn(aResponse()
                                            .withBody("foobar")))

        assertFalse(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere null hvis response er tom`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:4040"
        wireMockServer.stubFor(get(anyUrl())
                        .willReturn(aResponse()
                                            .withStatus(404)))

        assertNull(LeaderClient.isLeader())
    }
}
