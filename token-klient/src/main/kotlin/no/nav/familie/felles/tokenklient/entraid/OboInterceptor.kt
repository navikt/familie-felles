package no.nav.familie.felles.tokenklient.entraid

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class OboInterceptor(
    private val entraIDClient: EntraIDClient,
    private val target: String,
    private val tokenSupplier: () -> String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val oboToken = entraIDClient.hentOboToken(target, tokenSupplier())
        request.headers.setBearerAuth(oboToken)
        return execution.execute(request, body)
    }
}
