package no.nav.familie.http.interceptor

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.util.StopWatch

class TimingAndLoggingClientHttpRequestInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val timer = StopWatch()
        timer.start()
        val respons = execution.execute(request, body)
        timer.stop()
        log(request, respons.statusCode, timer)
        return respons
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TimingAndLoggingClientHttpRequestInterceptor::class.java)

        private fun log(
            request: HttpRequest,
            code: HttpStatusCode,
            timer: StopWatch,
        ) {
            if (hasError(code)) {
                LOG.warn("{} - {} - ({}). Dette tok {}ms", request.methodValue, request.uri, code, timer.totalTimeMillis)
            } else {
                LOG.info("{} - {} - ({}). Dette tok {}ms", request.methodValue, request.uri, code, timer.totalTimeMillis)
            }
        }

        private fun hasError(code: HttpStatusCode): Boolean {
            return code.is4xxClientError || code.is5xxServerError
        }
    }
}
