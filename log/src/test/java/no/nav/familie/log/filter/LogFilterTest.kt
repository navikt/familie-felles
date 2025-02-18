package no.nav.familie.log.filter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.familie.log.NavHttpHeaders
import no.nav.familie.log.NavSystemtype
import no.nav.familie.log.mdc.MDCConstants.MDC_USER_ID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class LogFilterTest {
    private lateinit var httpServletRequest: HttpServletRequest
    private lateinit var httpServletResponse: HttpServletResponse
    private val logFilter = LogFilter(systemtype = NavSystemtype.NAV_SAKSBEHANDLINGSSYSTEM)
    private val logFilterForEksternBrukerflate = LogFilter(systemtype = NavSystemtype.NAV_EKSTERN_BRUKERFLATE)
    private val logFilterForIntegrasjon = LogFilter(systemtype = NavSystemtype.NAV_INTEGRASJON)

    @BeforeEach
    fun setup() {
        httpServletRequest = mockHttpServletRequest
        httpServletResponse = mockHttpServletResponse
    }

    @Test
    fun cleanupOfMDCContext() {
        val initialContextMap =
            MDC.getCopyOfContextMap() ?: HashMap()

        logFilter.doFilter(
            httpServletRequest,
            httpServletResponse,
        ) { _, _ -> }

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

    @Test
    fun `skal hente ut brukerIdToken hvis loggeren brukes av saksbehandlingsløsning og cookien finnes`() {
        logFilter.doFilter(mockHttpServletRequestMedCookie, httpServletResponse) { _, _ ->
            assertThat(MDC.get(MDC_USER_ID)).isEqualTo("1234")
            verify(exactly = 0) { httpServletResponse.addCookie(any()) }
        }
    }

    @Test
    fun `skal generere brukerIdToken hvis loggeren brukes av saksbehandlingsløsning og cookien ikke finnes fra før av`() {
        logFilter.doFilter(mockHttpServletRequest, httpServletResponse) { _, _ ->
            verify(exactly = 1) { httpServletResponse.addCookie(any()) }
        }
    }

    @Test
    fun `skal ikke generere brukerIdToken hvis loggeren brukes av ekstern brukerflate`() {
        logFilterForEksternBrukerflate.doFilter(mockHttpServletRequest, httpServletResponse) { _, _ ->
            verify(exactly = 0) { httpServletResponse.addCookie(any()) }
        }
    }

    @Test
    fun `skal ikke hente ut brukerIdToken hvis loggeren brukes av ekstern brukerflate og cookie finnes`() {
        logFilterForEksternBrukerflate.doFilter(mockHttpServletRequestMedCookie, httpServletResponse) { _, _ ->
            verify(exactly = 0) { httpServletResponse.addCookie(any()) }
            assertThat(MDC.get(MDC_USER_ID)).isNull()
        }
    }

    @Test
    fun `skal ikke generere brukerIdToken hvis loggeren brukes av integrasjoner og cookien ikke finnes`() {
        logFilterForIntegrasjon.doFilter(mockHttpServletRequest, httpServletResponse) { _, _ ->
            verify(exactly = 0) { httpServletResponse.addCookie(any()) }
        }
    }

    @Test
    fun `skal generere brukerIdToken hvis loggeren brukes av integrasjoner og cookien allerede finnes`() {
        logFilterForIntegrasjon.doFilter(mockHttpServletRequestMedCookie, httpServletResponse) { _, _ ->
            verify(exactly = 0) { httpServletResponse.addCookie(any()) }
            assertThat(MDC.get(MDC_USER_ID)).isEqualTo("1234")
        }
    }

    companion object {
        private fun fail(): Unit = throw IllegalStateException("")

        private val mockHttpServletRequest: HttpServletRequest
            get() {
                val method = "GET"
                val requestUri = "/test/path"
                val request: HttpServletRequest = mockk(relaxed = true)
                every { request.method } returns method
                every { request.requestURI } returns requestUri
                return request
            }

        private val mockHttpServletRequestMedCookie: HttpServletRequest
            get() {
                val method = "GET"
                val requestUri = "/test/path"
                val request: HttpServletRequest = mockk(relaxed = true)
                every { request.method } returns method
                every { request.requestURI } returns requestUri
                every { request.cookies } returns arrayOf(Cookie("RUIDC", "1234"))
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
