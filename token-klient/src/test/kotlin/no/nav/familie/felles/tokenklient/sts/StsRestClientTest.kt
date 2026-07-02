package no.nav.familie.felles.tokenklient.sts

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate

class StsRestClientTest {
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var stsRestClient: StsRestClient

    private val stsUrl = "http://sts-server/token"

    @BeforeEach
    fun setUp() {
        val restTemplate = RestTemplate()
        mockServer = MockRestServiceServer.createServer(restTemplate)
        stsRestClient =
            StsRestClient(
                stsUrl = stsUrl,
                stsUsername = "username",
                stsPassword = "password",
                restClientBuilder =
                    org.springframework.web.client.RestClient
                        .builder(restTemplate),
            )
    }

    @Test
    fun `skal hente token fra STS og bruke cache på neste kall`() {
        mockServer
            .expect(requestTo(stsUrl))
            .andRespond(
                withSuccess(
                    """{"access_token": "token1", "token_type": "Bearer", "expires_in": 3600}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")

        // Andre kall skal bruke cache — ingen ny forventning registrert
        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")

        mockServer.verify()
    }

    @Test
    fun `skal hente nytt token når cachet token er utløpt`() {
        mockServer
            .expect(requestTo(stsUrl))
            .andRespond(
                withSuccess(
                    """{"access_token": "token1", "token_type": "Bearer", "expires_in": 1}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
        mockServer
            .expect(requestTo(stsUrl))
            .andRespond(
                withSuccess(
                    """{"access_token": "token2", "token_type": "Bearer", "expires_in": 1}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token1")

        Thread.sleep(1000)

        assertThat(stsRestClient.systemOIDCToken).isEqualTo("token2")

        mockServer.verify()
    }

    @Test
    fun `skal sende Basic Auth-header`() {
        mockServer
            .expect(requestTo(stsUrl))
            .andExpect(
                org.springframework.test.web.client.match.MockRestRequestMatchers
                    .header(
                        "Authorization",
                        "Basic dXNlcm5hbWU6cGFzc3dvcmQ=", // Base64("username:password")
                    ),
            ).andRespond(
                withSuccess(
                    """{"access_token": "token1", "token_type": "Bearer", "expires_in": 3600}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        stsRestClient.systemOIDCToken

        mockServer.verify()
    }
}
