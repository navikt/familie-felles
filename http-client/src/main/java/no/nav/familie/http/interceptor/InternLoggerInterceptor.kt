package no.nav.familie.http.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.log.auditlogger.AuditLogger
import no.nav.familie.sikkerhet.OIDCUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
@Import(OIDCUtil::class)
class InternLoggerInterceptor(
    private val oidcUtil: OIDCUtil,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val ansvarligSaksbehandler: String = hentSaksbehandler(oidcUtil)

        AuditLogger.logRequest(request, ansvarligSaksbehandler)
        LOG.info("[pre-handle] $ansvarligSaksbehandler - ${request.method}: ${request.requestURI}")
        return super.preHandle(request, response, handler)
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?,
    ) {
        postLogRequest(request, response, hentSaksbehandler(oidcUtil))
        super.postHandle(request, response, handler, modelAndView)
    }

    private fun postLogRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        ansvarligSaksbehandler: String,
    ) {
        val melding = "[post-handle] $ansvarligSaksbehandler - ${request.method}: ${request.requestURI} (${response.status})"

        if (HttpStatus.valueOf(response.status).isError) {
            LOG.warn(melding)
        } else {
            LOG.info(melding)
        }
    }

    private fun hentSaksbehandler(oidcUtil: OIDCUtil) =
        Result.runCatching { oidcUtil.getClaim("preferred_username") }.fold(
            onSuccess = { it },
            onFailure = { BRUKERNAVN_MASKINKALL },
        )

    companion object {
        private val LOG = LoggerFactory.getLogger(InternLoggerInterceptor::class.java)
        private const val BRUKERNAVN_MASKINKALL = "VL"
    }
}
