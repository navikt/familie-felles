package no.nav.familie.felles.texas

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class TexasMaskinTokenInterceptor(
    private val texasClient: TexasClient,
    private val target: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val token = texasClient.hentMaskinToken(target)
        request.headers.setBearerAuth(token)
        return execution.execute(request, body)
    }
}
