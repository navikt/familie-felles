package no.nav.familie.metrikker

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.servlet.AsyncHandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

/**
 * Sett property familie.tellAPIEndepunkterIBruk.enabled: true i application.yaml for å skru
 * på metrikker for hvor mange ganger et endepunkt har blitt kalt
 *
 * For å ekskludere endepunkter så kan man bruke
 * familie.tellAPIEndepunkterIBruk.ekskluder: /foo,/bar
 *
 */
@Component
@ConditionalOnProperty("familie.tellAPIEndepunkterIBruk.enabled")
class TellAPIEndepunkterIBrukInterceptor : AsyncHandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val path = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String
        TellAPIEndepunkterIBrukInitialiserer.metrikkerForEndepunkter.get("[${request.method}]$path")?.increment()
        return super.preHandle(request, response, handler)
    }
}
