package no.nav.familie.log.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletException
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.log.IdUtils
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.mdc.MDCConstants.MDC_CALL_ID
import no.nav.familie.log.mdc.MDCConstants.MDC_CONSUMER_ID
import no.nav.familie.log.mdc.MDCConstants.MDC_REQUEST_ID
import no.nav.familie.log.mdc.MDCConstants.MDC_USER_ID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.EOFException
import java.io.IOException
import java.util.function.Supplier

class LogFilter(
    /**
     * Filter init param used to specify a [&lt;Boolean&gt;][Supplier]
     * that will return whether stacktraces should be exposed or not
     * Defaults to always false
     */
    private val exposeErrorDetails: Supplier<Boolean> = Supplier { false },
    private val serverName: String? = null
) : HttpFilter() {

    @Throws(ServletException::class, IOException::class)
    override fun doFilter(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val userId = resolveUserId(httpServletRequest)
        if (userId == null || userId.isEmpty()) {
            // user-id tracking only works if the client is stateful and supports cookies.
            // if no user-id is found, generate one for any following requests but do not use it on the
            // current request to avoid generating large numbers of useless user-ids.
            generateUserIdCookie(httpServletResponse)
        }
        val consumerId = httpServletRequest.getHeader(NavHttpHeaders.NAV_CONSUMER_ID.asString())
        val callId = resolveCallId(httpServletRequest)
        MDC.put(MDC_CALL_ID, callId)
        MDC.put(MDC_USER_ID, userId)
        MDC.put(MDC_CONSUMER_ID, consumerId)
        MDC.put(MDC_REQUEST_ID, resolveRequestId(httpServletRequest))
        httpServletResponse.setHeader(NavHttpHeaders.NAV_CALL_ID.asString(), callId)
        if (serverName != null) {
            httpServletResponse.setHeader("Server", serverName)
        }
        try {
            filterWithErrorHandling(httpServletRequest, httpServletResponse, filterChain)
        } finally {
            MDC.remove(MDC_CALL_ID)
            MDC.remove(MDC_USER_ID)
            MDC.remove(MDC_CONSUMER_ID)
            MDC.remove(MDC_REQUEST_ID)
        }
    }

    @Throws(IOException::class, ServletException::class)
    private fun filterWithErrorHandling(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse)
        } catch (e: Exception) {
            if (e is EOFException) {
                log.warn(e.message, e)
            } else {
                log.error(e.message, e)
                if (httpServletResponse.isCommitted) {
                    log.error("failed with status={}", httpServletResponse.status)
                    throw e
                }
                httpServletResponse.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                if (exposeErrorDetails.get()) {
                    e.printStackTrace(httpServletResponse.writer)
                }
            }
        }
    }

    override fun init(filterConfig: FilterConfig) {}

    override fun destroy() {}

    companion object {
        // there is no consensus in NAV about header-names for correlation ids, so we support 'em all!
        // https://nav-it.slack.com/archives/C9UQ16AH4/p1538488785000100
        private val NAV_CALL_ID_HEADER_NAMES =
            listOf(
                NavHttpHeaders.NAV_CALL_ID.asString(),
                "Nav-CallId",
                "Nav-Callid",
                "X-Correlation-Id"
            )

        private val NAV_REQUEST_ID_HEADER_NAMES =
            listOf(
                "X_Request_Id",
                "X-Request-Id"
            )
        private val log = LoggerFactory.getLogger(LogFilter::class.java)
        private const val RANDOM_USER_ID_COOKIE_NAME = "RUIDC"
        private const val ONE_MONTH_IN_SECONDS = 60 * 60 * 24 * 30

        private fun resolveCallId(httpServletRequest: HttpServletRequest): String {
            return NAV_CALL_ID_HEADER_NAMES
                .mapNotNull { httpServletRequest.getHeader(it) }
                .firstOrNull { it.isNotEmpty() }
                ?: IdUtils.generateId()
        }

        private fun resolveRequestId(httpServletRequest: HttpServletRequest): String {
            return NAV_REQUEST_ID_HEADER_NAMES
                .mapNotNull { httpServletRequest.getHeader(it) }
                .firstOrNull { it.isNotEmpty() }
                ?: IdUtils.generateId()
        }

        private fun generateUserIdCookie(httpServletResponse: HttpServletResponse) {
            val userId = IdUtils.generateId()
            val cookie = Cookie(RANDOM_USER_ID_COOKIE_NAME, userId).apply {
                path = "/"
                maxAge = ONE_MONTH_IN_SECONDS
                isHttpOnly = true
                secure = true
            }
            httpServletResponse.addCookie(cookie)
        }

        private fun resolveUserId(httpServletRequest: HttpServletRequest): String? {
            return httpServletRequest.cookies?.firstOrNull { it -> RANDOM_USER_ID_COOKIE_NAME == it.name }?.value
        }
    }
}
