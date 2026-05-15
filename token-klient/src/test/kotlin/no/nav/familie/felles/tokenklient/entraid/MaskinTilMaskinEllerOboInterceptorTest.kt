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
import java.util.Base64

class MaskinTilMaskinEllerOboInterceptorTest {
    private val entraIDClient = mockk<EntraIDClient>()
    private val execution = mockk<ClientHttpRequestExecution>()
    private val response = mockk<ClientHttpResponse>()

    private fun lagJwtMedClaims(claims: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"RS256"}""".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toByteArray())
        return "$header.$payload.signatur"
    }

    private val brukerToken = lagJwtMedClaims("""{"preferred_username":"bruker@nav.no","sub":"123"}""")
    private val systemToken = lagJwtMedClaims("""{"sub":"system-client","oid":"abc"}""")

    @Test
    fun `skal bruke OBO når token har preferred_username`() {
        val target = "api://my-api/.default"
        val oboToken = "obo-access-token"
        val request = MockClientHttpRequest(HttpMethod.GET, "/test")
        val body = ByteArray(0)

        every { entraIDClient.hentOboToken(target, brukerToken) } returns oboToken
        every { execution.execute(request, body) } returns response

        val interceptor = MaskinTilMaskinEllerOboInterceptor(entraIDClient, target) { brukerToken }
        interceptor.intercept(request, body, execution)

        Assertions.assertEquals("Bearer $oboToken", request.headers.getFirst("Authorization"))
        verify(exactly = 1) { entraIDClient.hentOboToken(target, brukerToken) }
        verify(exactly = 0) { entraIDClient.hentMaskinTilMaskinToken(any()) }
    }

    @Test
    fun `skal bruke client credentials når token mangler preferred_username`() {
        val target = "api://my-api/.default"
        val m2mToken = "maskin-til-maskin-token"
        val request = MockClientHttpRequest(HttpMethod.GET, "/test")
        val body = ByteArray(0)

        every { entraIDClient.hentMaskinTilMaskinToken(target) } returns m2mToken
        every { execution.execute(request, body) } returns response

        val interceptor = MaskinTilMaskinEllerOboInterceptor(entraIDClient, target) { systemToken }
        interceptor.intercept(request, body, execution)

        Assertions.assertEquals("Bearer $m2mToken", request.headers.getFirst("Authorization"))
        verify(exactly = 1) { entraIDClient.hentMaskinTilMaskinToken(target) }
        verify(exactly = 0) { entraIDClient.hentOboToken(any(), any()) }
    }

    @Test
    fun `skal bruke client credentials når tokenSupplier returnerer null`() {
        val target = "api://my-api/.default"
        val m2mToken = "maskin-til-maskin-token"
        val request = MockClientHttpRequest(HttpMethod.GET, "/test")
        val body = ByteArray(0)

        every { entraIDClient.hentMaskinTilMaskinToken(target) } returns m2mToken
        every { execution.execute(request, body) } returns response

        val interceptor = MaskinTilMaskinEllerOboInterceptor(entraIDClient, target) { null }
        interceptor.intercept(request, body, execution)

        Assertions.assertEquals("Bearer $m2mToken", request.headers.getFirst("Authorization"))
        verify(exactly = 1) { entraIDClient.hentMaskinTilMaskinToken(target) }
        verify(exactly = 0) { entraIDClient.hentOboToken(any(), any()) }
    }

    @Test
    fun `skal kalle execution videre uavhengig av grant type`() {
        val target = "api://my-api/.default"
        val request = MockClientHttpRequest(HttpMethod.POST, "/api")
        val body = "{}".toByteArray()

        every { entraIDClient.hentMaskinTilMaskinToken(target) } returns "token"
        every { execution.execute(request, body) } returns response

        val interceptor = MaskinTilMaskinEllerOboInterceptor(entraIDClient, target) { null }
        val result = interceptor.intercept(request, body, execution)

        verify(exactly = 1) { execution.execute(request, body) }
        Assertions.assertEquals(response, result)
    }

    @Test
    fun `skal evaluere token på nytt for hver request`() {
        val target = "api://my-api/.default"
        val request1 = MockClientHttpRequest(HttpMethod.GET, "/1")
        val request2 = MockClientHttpRequest(HttpMethod.GET, "/2")
        val body = ByteArray(0)
        var kall = 0

        every { entraIDClient.hentOboToken(target, brukerToken) } returns "obo-token"
        every { entraIDClient.hentMaskinTilMaskinToken(target) } returns "m2m-token"
        every { execution.execute(any(), body) } returns response

        val interceptor = MaskinTilMaskinEllerOboInterceptor(entraIDClient, target) { if (++kall == 1) brukerToken else systemToken }
        interceptor.intercept(request1, body, execution)
        interceptor.intercept(request2, body, execution)

        Assertions.assertEquals("Bearer obo-token", request1.headers.getFirst("Authorization"))
        Assertions.assertEquals("Bearer m2m-token", request2.headers.getFirst("Authorization"))
    }
}
