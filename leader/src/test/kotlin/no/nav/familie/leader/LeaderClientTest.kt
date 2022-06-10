package no.nav.familie.leader

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.InetAddress

class LeaderClientTest {

    companion object {

        private lateinit var wireMockServer: WireMockServer

        @BeforeAll
        @JvmStatic
        fun initClass() {
            wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
            wireMockServer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            wireMockServer.stop()
        }
    }

    @AfterEach
    fun tearDownEachTest() {
        wireMockServer.resetAll()
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
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(
            get(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("{\"name\": \"${InetAddress.getLocalHost().hostName}\"}")
                )
        )

        assertTrue(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere false hvis pod ikke er leader`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(
            get(anyUrl())
                .willReturn(
                    aResponse()
                        .withBody("foobar")
                )
        )

        assertFalse(LeaderClient.isLeader()!!)
    }

    @Test
    fun `Skal returnere null hvis response er tom`() {
        mockkStatic(Environment::class)
        every { Environment.hentLeaderSystemEnv() } returns "localhost:${wireMockServer.port()}"
        wireMockServer.stubFor(
            get(anyUrl())
                .willReturn(
                    aResponse()
                        .withStatus(404)
                )
        )

        assertNull(LeaderClient.isLeader())
    }
}
