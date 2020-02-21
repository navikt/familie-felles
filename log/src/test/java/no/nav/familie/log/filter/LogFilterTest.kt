package no.nav.familie.log.filter

import no.nav.familie.log.NavHttpHeaders
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
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
                val request = Mockito.mock(HttpServletRequest::class.java)
                Mockito.`when`(request.method).thenReturn(method)
                Mockito.`when`(request.requestURI).thenReturn(requestUri)
                return request
            }

        private val mockHttpServletResponse: HttpServletResponse
            get() {
                val response =
                        Mockito.mock(HttpServletResponse::class.java)
                val headers: MutableMap<String, String> = HashMap()
                val status = intArrayOf(0)
                val statusAnswer =
                        Answer<Void?> { invocationOnMock: InvocationOnMock ->
                            status[0] = invocationOnMock.getArgument(0)
                            null
                        }
                Mockito.doAnswer(statusAnswer).`when`(response).status = ArgumentMatchers.anyInt()
                val headerAnswer =
                        Answer<Void?> { invocationOnMock: InvocationOnMock ->
                            headers[invocationOnMock.getArgument(0)] = invocationOnMock.getArgument(1)
                            null
                        }
                Mockito.doAnswer(headerAnswer).`when`(response)
                        .setHeader(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
                Mockito.`when`(response.status).thenAnswer { status[0] }
                Mockito.doAnswer { invocationOnMock: InvocationOnMock ->
                    val s = invocationOnMock.getArgument<String>(0)
                    headers[s]
                }.`when`(response)
                        .getHeader(ArgumentMatchers.anyString())
                Mockito.`when`(response.headerNames)
                        .thenAnswer { headers.keys }
                return response
            }
    }
}
