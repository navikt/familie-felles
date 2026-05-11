package no.nav.familie.felles.texas

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

class TexasClientTest {
    private val tokenEndpoint = "http://texas-mock/token"

    private fun lagClientOgServer(): Pair<TexasClient, MockRestServiceServer> {
        val restTemplate = RestTemplate()
        val mockServer = MockRestServiceServer.createServer(restTemplate)
        val restClient = RestClient.create(restTemplate)
        val client = TexasClient(tokenEndpoint, restClient)
        return Pair(client, mockServer)
    }

    @Test
    fun `skal returnere access_token fra velykket respons`() {
        val (client, mockServer) = lagClientOgServer()

        mockServer
            .expect(requestTo(tokenEndpoint))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"access_token":"mitt-token","expires_in":3600,"token_type":"Bearer"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val token = client.hentMaskinTilMaskinToken("api://min-tjeneste/.default")

        assertEquals("mitt-token", token)
        mockServer.verify()
    }

    @Test
    fun `skal sende identity_provider og target i request-body`() {
        val (client, mockServer) = lagClientOgServer()
        val scope = "api://min-tjeneste/.default"

        mockServer
            .expect(requestTo(tokenEndpoint))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"identity_provider\":\"entra_id\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"target\":\"$scope\"")))
            .andRespond(
                withSuccess(
                    """{"access_token":"token","expires_in":3600,"token_type":"Bearer"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        client.hentMaskinTilMaskinToken(scope)

        mockServer.verify()
    }
}
