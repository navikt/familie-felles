package no.nav.familie.http.interceptor

import no.nav.familie.log.IdUtils
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class MdcValuesPropagatingClientInterceptor(val consumerId: String) : ClientHttpRequestInterceptor {

    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {

        val consumerId = MDC.get(MDCConstants.MDC_CONSUMER_ID) ?: consumerId
        request.headers.add(NavHttpHeaders.NAV_CONSUMER_ID.asString(), consumerId)
        val callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
        request.headers.add(NavHttpHeaders.NAV_CALL_ID.asString(), callId)

        return execution.execute(request, body)
    }
}
