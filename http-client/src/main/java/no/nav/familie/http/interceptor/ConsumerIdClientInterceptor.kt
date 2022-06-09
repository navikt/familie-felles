package no.nav.familie.http.interceptor

import no.nav.familie.log.NavHttpHeaders
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class ConsumerIdClientInterceptor(
    @Value("\${application.name}") private val appName: String,
    @Value("\${credential.username:}") private val serviceUser: String
) :
    ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {

        request.headers.add(NavHttpHeaders.NAV_CONSUMER_ID.asString(), if (!serviceUser.isBlank()) serviceUser else appName)
        return execution.execute(request, body)
    }
}
