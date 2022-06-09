package no.nav.familie.log.filter

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.util.StopWatch
import java.io.IOException
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

open class RequestTimeFilter : Filter {

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain) {
        val response = servletResponse as HttpServletResponse
        val request = servletRequest as HttpServletRequest
        val timer = StopWatch()
        try {
            timer.start()
            filterChain.doFilter(servletRequest, servletResponse)
        } finally {
            timer.stop()
            log(request, response.status, timer)
        }
    }

    private fun log(request: HttpServletRequest, code: Int, timer: StopWatch) {
        if (HttpStatus.valueOf(code).isError) {
            LOG.warn("{} - {} - ({}). Dette tok {}ms", request.method, request.requestURI, code, timer.totalTimeMillis)
        } else {
            if (!shouldNotFilter(request.requestURI)) {
                LOG.info("{} - {} - ({}). Dette tok {}ms", request.method, request.requestURI, code, timer.totalTimeMillis)
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate") // kan overrides hvis det ønskes
    fun shouldNotFilter(uri: String): Boolean {
        return uri.contains("/internal") || uri == "/api/ping"
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(RequestTimeFilter::class.java)
    }
}
