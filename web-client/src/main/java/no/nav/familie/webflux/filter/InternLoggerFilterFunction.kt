package no.nav.familie.webflux.filter

import no.nav.familie.log.auditlogger.AuditLogger
import no.nav.familie.sikkerhet.OIDCUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import reactor.core.publisher.SignalType
import java.util.concurrent.atomic.AtomicBoolean

@Component
@Import(OIDCUtil::class)
class InternLoggerFilterFunction(private val oidcUtil: OIDCUtil) : ExchangeFilterFunction {


    override fun filter(request: ClientRequest, function: ExchangeFunction): Mono<ClientResponse> {
        preHandle(request)
        return function.exchange(request).`as` { responseMono: Mono<ClientResponse> -> postHandle(request, responseMono) }
    }


    fun preHandle(request: ClientRequest) {
        val ansvarligSaksbehandler: String = hentSaksbehandler(oidcUtil)

        AuditLogger.logRequest(request, ansvarligSaksbehandler)
        println("prehandle")
        LOG.info("[pre-handle] $ansvarligSaksbehandler - ${request.method()}: ${request.url()}")
    }

    private fun postLogRequest(request: ClientRequest,
                               signalType: SignalType,
                               ansvarligSaksbehandler: String) {

        val melding = "[post-handle] $ansvarligSaksbehandler - ${request.method()}: ${request.url()} (${signalType})"

        if (signalType == SignalType.ON_ERROR) {
            LOG.warn(melding)
        } else {
            LOG.info(melding)
        }
    }

    private fun postHandle(request: ClientRequest, responseMono: Mono<ClientResponse>): Mono<ClientResponse> {
        println("posthandle")

        val responseReceived = AtomicBoolean()
        return Mono.defer {
            responseMono.doOnEach { signal: Signal<ClientResponse> ->
                if (signal.isOnNext || signal.isOnError) {
                    responseReceived.set(true)
                    postLogRequest(request, signal.type, hentSaksbehandler(oidcUtil))
                }
            }.doFinally { signalType: SignalType ->
                if (!responseReceived.get() && SignalType.CANCEL == signalType) {
                    postLogRequest(request, signalType, hentSaksbehandler(oidcUtil))
                }
            }
        }
    }

    private fun hentSaksbehandler(oidcUtil: OIDCUtil) =
            Result.runCatching { oidcUtil.getClaim("preferred_username") }.fold(
                    onSuccess = { it },
                    onFailure = { BRUKERNAVN_MASKINKALL }
            )

    companion object {

        private val LOG = LoggerFactory.getLogger(InternLoggerFilterFunction::class.java)
        private const val BRUKERNAVN_MASKINKALL = "VL"
    }
}
