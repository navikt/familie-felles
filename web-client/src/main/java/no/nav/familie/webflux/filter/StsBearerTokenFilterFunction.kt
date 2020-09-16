package no.nav.familie.webflux.filter

import no.nav.familie.http.sts.StsRestClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class StsBearerTokenFilterFunction(private val stsRestClient: StsRestClient) : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, execution: ExchangeFunction): Mono<ClientResponse> {
        val systembrukerToken = stsRestClient.systemOIDCToken
        request.headers().setBearerAuth(systembrukerToken)
        return execution.exchange(request)
    }

}

