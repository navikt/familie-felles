package no.nav.familie.http.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

import java.net.URI

@Component
class ApiKeyInjectingClientInterceptor(private val apiKeys: Map<URI, Pair<String, String>>) : ClientHttpRequestInterceptor {

    private val logger = LoggerFactory.getLogger(ApiKeyInjectingClientInterceptor::class.java)

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        val apiKey = apiKeyFor(request.uri)
        if (apiKey != null) {
            logger.trace("Injisert API-key som header {} for {}", apiKey.first, request.uri)
            request.headers.add(apiKey.first, apiKey.second)
        } else {
            logger.trace("Ingen API-key ble funnet for {} (sjekket {} konfigurasjoner)", request.uri,
                         apiKeys.values.size)
        }
        return execution.execute(request, body)
    }

    private fun apiKeyFor(uri: URI): Pair<String, String>? {
        return apiKeys.entries
                .filter { s -> uri.toString().startsWith(s.key.toString()) }
                .map { it.value }
                .firstOrNull()
    }
}

