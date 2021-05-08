package no.nav.familie.http.client

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.*
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Abstract klasse for å kalle rest-tjenester med metrics og utpakking av ev. body.
 */
abstract class AbstractRestClient(val operations: RestOperations,
                                  metricsPrefix: String) {

    protected val responstid: Timer = Metrics.timer("$metricsPrefix.tid")
    protected val responsSuccess: Counter = Metrics.counter("$metricsPrefix.response", "status", "success")
    protected val responsFailure: Counter = Metrics.counter("$metricsPrefix.response", "status", "failure")

    protected val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

    protected inline fun <reified T : Any> getForEntity(uri: URI): T {
        return getForEntity(uri, null)
    }

    protected inline fun <reified T : Any> getForEntity(uri: URI, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.GET, HttpEntity(null, httpHeaders)) }
    }

    protected inline fun <reified T : Any> postForEntity(uri: URI, payload: Any): T {
        return postForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> postForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.POST, HttpEntity(payload, httpHeaders)) }
    }

    protected inline fun <reified T : Any> putForEntity(uri: URI, payload: Any): T {
        return putForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> putForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.PUT, HttpEntity(payload, httpHeaders)) }
    }

    protected inline fun <reified T : Any> patchForEntity(uri: URI, payload: Any): T {
        return patchForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> patchForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.PATCH, HttpEntity(payload, httpHeaders)) }
    }

    protected inline fun <reified T : Any> deleteForEntity(uri: URI, payload: Any? = null): T {
        return deleteForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> deleteForEntity(uri: URI, payload: Any?, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) { operations.exchange<T>(uri, HttpMethod.DELETE, HttpEntity(payload, httpHeaders)) }
    }


    private fun <T> validerOgPakkUt(respons: ResponseEntity<T>, uri: URI): T {
        if (!respons.statusCode.is2xxSuccessful) {
            secureLogger.info("Kall mot $uri feilet:  ${respons.body}")
            log.info("Kall mot $uri feilet: ${respons.statusCode}")
            throw HttpServerErrorException(respons.statusCode, "", respons.body?.toString()?.toByteArray(), Charsets.UTF_8)
        }
        return respons.body as T
    }

    protected fun <T> executeMedMetrics(uri: URI, function: () -> ResponseEntity<T>): T {
        try {
            val startTime = System.nanoTime()
            val responseEntity = function.invoke()
            responstid.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            responsSuccess.increment()
            return validerOgPakkUt(responseEntity, uri)
        } catch (e: RestClientResponseException) {
            responsFailure.increment()
            secureLogger.error("RestClientResponseException ved kall mot uri=$uri", e)
            throw e
        } catch (e: HttpClientErrorException) {
            responsFailure.increment()
            secureLogger.error("HttpClientErrorException ved kall mot uri=$uri", e)
            throw e
        } catch (e: Exception) {
            responsFailure.increment()
            secureLogger.error("Feil ved kall mot uri=$uri", e)
            throw RuntimeException("Feil ved kall mot uri=$uri", e)
        }
    }

    override fun toString(): String = this::class.simpleName + " [operations=" + operations + "]"

}
