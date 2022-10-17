package no.nav.familie.webflux.filter

import no.nav.familie.webflux.sts.StsTokenClient
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Import(StsTokenClient::class)
@Component
class StsBearerTokenFilter(private val stsTokenClient: StsTokenClient) : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        val systembrukerToken = stsTokenClient.systemOIDCToken
        val modifiedRequest = ClientRequest.from(request).header("Authorization", "Bearer $systembrukerToken").build()
        return function.exchange(modifiedRequest)
    }
}
