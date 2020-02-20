package no.nav.familie.http.interceptor

import no.nav.familie.http.auditlogger.AuditLogger
import no.nav.familie.sikkerhet.OIDCUtil
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class InternLoggerInterceptor(private val oidcUtil: OIDCUtil) : HandlerInterceptorAdapter() {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ansvarligSaksbehandler: String = hentSaksbehandler(oidcUtil = oidcUtil) ?: "VL"

        AuditLogger.logRequest(request, ansvarligSaksbehandler)
        logRequest(request, response, ansvarligSaksbehandler)
        return super.preHandle(request, response, handler)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InternLoggerInterceptor::class.java)

        private fun logRequest(request: HttpServletRequest,
                               response: HttpServletResponse,
                               ansvarligSaksbehandler: String?) {
            val melding = "$ansvarligSaksbehandler - ${request.method}: ${request.requestURI} (${response.status})"
            if (hasError(response.status)) {
                LOG.warn(melding)
            } else {
                LOG.info(melding)
            }
        }

        private fun hasError(status: Int): Boolean {
            return status >= 400
        }

        private fun hentSaksbehandler(oidcUtil: OIDCUtil) = Result.runCatching { oidcUtil.getClaim("preferred_username") }.fold(
                onSuccess = { it },
                onFailure = { null }
        )
    }
}
