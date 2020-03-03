package no.nav.familie.http.interceptor

import no.nav.familie.log.auditlogger.AuditLogger
import no.nav.familie.sikkerhet.OIDCUtil
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class InternLoggerInterceptor(private val oidcUtil: OIDCUtil) : HandlerInterceptorAdapter() {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ansvarligSaksbehandler: String = hentSaksbehandler(oidcUtil = oidcUtil) ?: "VL"

        AuditLogger.logRequest(request, ansvarligSaksbehandler)
        logRequest(Handler.PRE_HANDLE, request, response, ansvarligSaksbehandler)
        return super.preHandle(request, response, handler)
    }

    override fun postHandle(request: HttpServletRequest,
                            response: HttpServletResponse,
                            handler: Any,
                            modelAndView: ModelAndView?) {
        val ansvarligSaksbehandler: String = hentSaksbehandler(oidcUtil = oidcUtil) ?: "VL"

        logRequest(Handler.POST_HANDLE, request, response, ansvarligSaksbehandler)
        super.postHandle(request, response, handler, modelAndView)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InternLoggerInterceptor::class.java)

        private fun logRequest(
                handler: Handler,
                request: HttpServletRequest,
                response: HttpServletResponse,
                ansvarligSaksbehandler: String?) {
            var melding =
                    "[${handler.logMelding}] $ansvarligSaksbehandler - ${request.method}: ${request.requestURI}"

            if (handler == Handler.POST_HANDLE) {
                melding += " (${response.status})"
            }

            if (handler == Handler.POST_HANDLE && HttpStatus.valueOf(response.status).isError) {
                LOG.warn(melding)
            } else {
                LOG.info(melding)
            }
        }

        private fun hentSaksbehandler(oidcUtil: OIDCUtil) = Result.runCatching { oidcUtil.getClaim("preferred_username") }.fold(
                onSuccess = { it },
                onFailure = { null }
        )
    }
}

enum class Handler(val logMelding: String) {
    PRE_HANDLE("pre-handle"), POST_HANDLE("post-handle")
}
