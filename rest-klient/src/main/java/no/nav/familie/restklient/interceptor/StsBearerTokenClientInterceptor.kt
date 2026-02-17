package no.nav.familie.restklient.interceptor

import no.nav.familie.restklient.sts.StsRestClient
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class StsBearerTokenClientInterceptor(
    private val stsRestClient: StsRestClient,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val systembrukerToken = stsRestClient.systemOIDCToken
        request.headers.setBearerAuth(systembrukerToken)
        return execution.execute(request, body)
    }
}
