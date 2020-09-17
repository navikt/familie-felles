package no.nav.familie.webflux.filter

import no.nav.familie.log.IdUtils
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class MdcValuesPropagatingFilterFunction : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, execution: ExchangeFunction): Mono<ClientResponse> {

        val callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
        request.headers().add(NavHttpHeaders.NAV_CALL_ID.asString(), callId)

        return execution.exchange(request)
    }
}
