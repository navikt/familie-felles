package no.nav.familie.log.filter

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.util.StopWatch
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RequestTimeFilter : Filter {

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
            if (!isHealthCheck(request.requestURI)) {
                LOG.info("{} - {} - ({}). Dette tok {}ms", request.method, request.requestURI, code, timer.totalTimeMillis)
            }
        }
    }

    private fun isHealthCheck(uri: String): Boolean {
        return uri.contains("/internal")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(RequestTimeFilter::class.java)
    }
}
