package no.nav.familie.felles.tokenklient.entraid

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class MaskinTilMaskinTokenInterceptor(
    private val entraIDClient: EntraIDClient,
    private val target: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val token = entraIDClient.hentMaskinTilMaskinToken(target)
        request.headers.setBearerAuth(token)
        return execution.execute(request, body)
    }
}
