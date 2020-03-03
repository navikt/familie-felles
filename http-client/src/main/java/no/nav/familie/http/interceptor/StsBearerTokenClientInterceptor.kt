package no.nav.familie.http.interceptor

import no.nav.familie.http.sts.StsRestClient
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class StsBearerTokenClientInterceptor(private val stsRestClient: StsRestClient) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        val systembrukerToken = stsRestClient.systemOIDCToken
        request.headers.setBearerAuth(systembrukerToken)
        return execution.execute(request, body)
    }

}

