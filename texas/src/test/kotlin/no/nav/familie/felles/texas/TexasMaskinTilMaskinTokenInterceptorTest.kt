package no.nav.familie.felles.texas

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest

class TexasMaskinTilMaskinTokenInterceptorTest {
    private val texasClient = mockk<TexasClient>()
    private val execution = mockk<ClientHttpRequestExecution>()
    private val response = mockk<ClientHttpResponse>()

    @Test
    fun `skal sette Bearer token på request-headeren`() {
        val target = "api://my-api/.default"
        val token = "test-access-token"
        val request = MockClientHttpRequest(HttpMethod.GET, "/test")
        val body = ByteArray(0)

        every { texasClient.hentMaskinTilMaskinToken(target) } returns token
        every { execution.execute(request, body) } returns response

        val interceptor = TexasMaskinTilMaskinTokenInterceptor(texasClient, target)
        interceptor.intercept(request, body, execution)

        assertEquals("Bearer $token", request.headers.getFirst("Authorization"))
    }

    @Test
    fun `skal kalle hentMaskinTilMaskinToken med riktig target`() {
        val target = "api://min-tjeneste/.default"
        val request = MockClientHttpRequest(HttpMethod.POST, "/api/data")
        val body = "{}".toByteArray()

        every { texasClient.hentMaskinTilMaskinToken(target) } returns "et-token"
        every { execution.execute(request, body) } returns response

        val interceptor = TexasMaskinTilMaskinTokenInterceptor(texasClient, target)
        interceptor.intercept(request, body, execution)

        verify(exactly = 1) { texasClient.hentMaskinTilMaskinToken(target) }
    }

    @Test
    fun `skal kalle execution videre etter å ha satt token`() {
        val target = "api://min-tjeneste/.default"
        val request = MockClientHttpRequest(HttpMethod.GET, "/")
        val body = ByteArray(0)

        every { texasClient.hentMaskinTilMaskinToken(target) } returns "et-token"
        every { execution.execute(request, body) } returns response

        val interceptor = TexasMaskinTilMaskinTokenInterceptor(texasClient, target)
        val result = interceptor.intercept(request, body, execution)

        verify(exactly = 1) { execution.execute(request, body) }
        assertEquals(response, result)
    }
}
