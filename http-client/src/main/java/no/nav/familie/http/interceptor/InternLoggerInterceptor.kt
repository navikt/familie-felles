package no.nav.familie.http.interceptor

import no.nav.familie.log.auditlogger.AuditLogger
import no.nav.familie.sikkerhet.OIDCUtil
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Import(OIDCUtil::class)
class InternLoggerInterceptor(private val oidcUtil: OIDCUtil) : ClientHttpRequestInterceptor {


    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {

        val ansvarligSaksbehandler: String = hentSaksbehandler(oidcUtil)
        AuditLogger.logRequest(request, ansvarligSaksbehandler)
        LOG.info("[pre-handle] $ansvarligSaksbehandler - ${request.method}: ${request.uri}")
        val response = execution.execute(request, body)
        postLogRequest(request, response, ansvarligSaksbehandler)
        return response
    }

    private fun postLogRequest(request: HttpRequest,
                               response: ClientHttpResponse,
                               ansvarligSaksbehandler: String) {

        val melding = "[post-handle] $ansvarligSaksbehandler - ${request.method}: ${request.uri} (${response.statusCode})"

        if (response.statusCode.isError) {
            LOG.warn(melding)
        } else {
            LOG.info(melding)
        }
    }

    private fun hentSaksbehandler(oidcUtil: OIDCUtil) =
            Result.runCatching { oidcUtil.getClaim("preferred_username") }.fold(
                    onSuccess = { it },
                    onFailure = { BRUKERNAVN_MASKINKALL }
            )

    companion object {

        private val LOG = LoggerFactory.getLogger(InternLoggerInterceptor::class.java)
        private const val BRUKERNAVN_MASKINKALL = "VL"
    }
}
