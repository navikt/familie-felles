package no.nav.familie.webflux.filter

import no.nav.familie.log.NavHttpHeaders
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class ConsumerIdFilterFunction(@Value("\${application.name}") private val appName: String,
                               @Value("\${credential.username:}") private val serviceUser: String) :
        ExchangeFilterFunction {

    override fun filter(request: ClientRequest, execution: ExchangeFunction): Mono<ClientResponse> {

        request.headers().add(NavHttpHeaders.NAV_CONSUMER_ID.asString(), if (!serviceUser.isBlank()) serviceUser else appName)
        return execution.exchange(request)
    }
}
