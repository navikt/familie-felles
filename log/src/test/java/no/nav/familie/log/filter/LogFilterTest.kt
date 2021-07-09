package no.nav.familie.log.filter

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.log.NavHttpHeaders
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.slf4j.MDC
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LogFilterTest {

    private lateinit var httpServletRequest: HttpServletRequest
    private lateinit var httpServletResponse: HttpServletResponse
    private val logFilter = LogFilter()

    @Before
    fun setup() {
        httpServletRequest = mockHttpServletRequest
        httpServletResponse = mockHttpServletResponse
    }

    @Test
    fun cleanupOfMDCContext() {
        val initialContextMap =
                MDC.getCopyOfContextMap() ?: HashMap()

        logFilter.doFilter(httpServletRequest,
                           httpServletResponse) { _, _ -> }

        assertThat(initialContextMap)
                .isEqualTo(MDC.getCopyOfContextMap() ?: HashMap<String, String>())
    }

    @Test
    fun addResponseHeaders() {
        logFilter.doFilter(httpServletRequest, httpServletResponse) { _, _ -> }

        assertThat(httpServletResponse.getHeader(NavHttpHeaders.NAV_CALL_ID.asString()))
                .isNotEmpty()
        assertThat(httpServletResponse.getHeader("Server")).isNull()
    }

    @Test
    fun handleExceptions() {
        logFilter.doFilter(httpServletRequest, httpServletResponse) { _, _ -> fail() }

        assertThat(httpServletResponse.status)
                .isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    }

    companion object {

        private fun fail() {
            throw IllegalStateException("")
        }

        private val mockHttpServletRequest: HttpServletRequest
            get() {
                val method = "GET"
                val requestUri = "/test/path"
                val request: HttpServletRequest = mockk(relaxed = true)
                every { request.method } returns method
                every { request.requestURI } returns requestUri
                return request
            }

        private val mockHttpServletResponse: HttpServletResponse
            get() {
                val response: HttpServletResponse = mockk(relaxed = true)
                val headers: MutableMap<String, String> = HashMap()
                val status = intArrayOf(0)
                every { response.status = any() } answers { status[0] = firstArg() }
                every { response.setHeader(any(), any()) } answers { headers[firstArg()] = secondArg() }
                every { response.status } answers { status[0] }
                every { response.getHeader(any()) } answers { headers[firstArg()] }
                every { response.headerNames } answers { headers.keys }
                return response
            }
    }
}
