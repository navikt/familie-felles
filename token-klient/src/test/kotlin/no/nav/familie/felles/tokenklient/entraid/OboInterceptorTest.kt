package no.nav.familie.felles.tokenklient.entraid

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest

class OboInterceptorTest {
    private val entraIDClient = mockk<EntraIDClient>()
    private val execution = mockk<ClientHttpRequestExecution>()
    private val response = mockk<ClientHttpResponse>()

    @Test
    fun `skal sette Bearer obo-token på request-headeren`() {
        val target = "api://my-api/.default"
        val brukerToken = "innkommende-bruker-token"
        val oboToken = "obo-access-token"
        val request = MockClientHttpRequest(HttpMethod.GET, "/test")
        val body = ByteArray(0)

        every { entraIDClient.hentOboToken(target, brukerToken) } returns oboToken
        every { execution.execute(request, body) } returns response

        val interceptor = OboInterceptor(entraIDClient, target) { brukerToken }
        interceptor.intercept(request, body, execution)

        Assertions.assertEquals("Bearer $oboToken", request.headers.getFirst("Authorization"))
    }

    @Test
    fun `skal kalle hentOboToken med riktig target og brukertoken`() {
        val target = "api://min-tjeneste/.default"
        val brukerToken = "bruker-jwt"
        val request = MockClientHttpRequest(HttpMethod.POST, "/api/data")
        val body = "{}".toByteArray()

        every { entraIDClient.hentOboToken(target, brukerToken) } returns "et-obo-token"
        every { execution.execute(request, body) } returns response

        val interceptor = OboInterceptor(entraIDClient, target) { brukerToken }
        interceptor.intercept(request, body, execution)

        verify(exactly = 1) { entraIDClient.hentOboToken(target, brukerToken) }
    }

    @Test
    fun `skal kalle execution videre etter å ha satt obo-token`() {
        val target = "api://min-tjeneste/.default"
        val request = MockClientHttpRequest(HttpMethod.GET, "/")
        val body = ByteArray(0)

        every { entraIDClient.hentOboToken(target, "bruker-token") } returns "et-obo-token"
        every { execution.execute(request, body) } returns response

        val interceptor = OboInterceptor(entraIDClient, target) { "bruker-token" }
        val result = interceptor.intercept(request, body, execution)

        verify(exactly = 1) { execution.execute(request, body) }
        Assertions.assertEquals(response, result)
    }

    @Test
    fun `skal hente brukertoken fra supplier ved hver request`() {
        val target = "api://min-tjeneste/.default"
        val request1 = MockClientHttpRequest(HttpMethod.GET, "/1")
        val request2 = MockClientHttpRequest(HttpMethod.GET, "/2")
        val body = ByteArray(0)
        var tokenKallTeller = 0

        every { entraIDClient.hentOboToken(target, "token-1") } returns "obo-1"
        every { entraIDClient.hentOboToken(target, "token-2") } returns "obo-2"
        every { execution.execute(any(), body) } returns response

        val interceptor = OboInterceptor(entraIDClient, target) { if (++tokenKallTeller == 1) "token-1" else "token-2" }
        interceptor.intercept(request1, body, execution)
        interceptor.intercept(request2, body, execution)

        Assertions.assertEquals("Bearer obo-1", request1.headers.getFirst("Authorization"))
        Assertions.assertEquals("Bearer obo-2", request2.headers.getFirst("Authorization"))
    }
}
