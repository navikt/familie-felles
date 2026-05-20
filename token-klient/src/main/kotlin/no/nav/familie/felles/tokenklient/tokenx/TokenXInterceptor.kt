package no.nav.familie.felles.tokenklient.tokenx

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class TokenXInterceptor(
    private val tokenXClient: TokenXClient,
    private val target: String,
    private val tokenSupplier: () -> String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val oboToken = tokenXClient.hentToken(target, tokenSupplier())
        request.headers.setBearerAuth(oboToken)
        return execution.execute(request, body)
    }
}
