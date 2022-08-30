package no.nav.familie.http.ecb.interceptor

import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class ECBRestClientInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        request.headers.accept = listOf(MediaType.APPLICATION_XML)
        request.headers.contentType = MediaType.APPLICATION_XML
        return execution.execute(request, body)
    }
}
