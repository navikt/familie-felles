package no.nav.familie.felles.tokenklient

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class TexasMaskinTilMaskinTokenInterceptor(
    private val texasClient: TexasClient,
    private val target: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val token = texasClient.hentMaskinTilMaskinToken(target)
        request.headers.setBearerAuth(token)
        return execution.execute(request, body)
    }
}
