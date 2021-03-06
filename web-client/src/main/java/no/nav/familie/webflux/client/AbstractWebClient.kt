package no.nav.familie.webflux.client

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Abstract klasse for å kalle webclient-tjenester med metrics og feil-logging.
 */
abstract class AbstractWebClient(val webClient: WebClient,
                                 metricsPrefix: String) {

    protected val responstid: Timer = Metrics.timer("$metricsPrefix.tid")
    protected val responsSuccess: Counter = Metrics.counter("$metricsPrefix.response", "status", "success")
    protected val responsFailure: Counter = Metrics.counter("$metricsPrefix.response", "status", "failure")

    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

    protected inline fun <reified T : Any> getForEntity(uri: URI): T {
        return getForEntity(uri, null)
    }

    protected inline fun <reified T : Any> getForEntity(uri: URI, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) {
            webClient.get()
                    .uri(uri)
                    .addHeaders<WebClient.RequestHeadersUriSpec<*>>(httpHeaders)
                    .retrieve()
                    .bodyToMono<T>()
        }
    }

    protected inline fun <reified T : Any> postForEntity(uri: URI, payload: Any): T {
        return postForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> postForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) {
            webClient.post()
                    .uri(uri)
                    .addHeaders<WebClient.RequestBodyUriSpec>(httpHeaders)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono<T>()
        }
    }

    protected inline fun <reified S : WebClient.RequestHeadersSpec<*>>
            WebClient.RequestHeadersSpec<*>.addHeaders(httpHeaders: HttpHeaders?): S {
        httpHeaders?.entries?.forEach { this.header(it.key, *it.value.toTypedArray()) }
        return this as S
    }

    protected inline fun <reified T : Any> putForEntity(uri: URI, payload: Any): T {
        return putForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> putForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) {
            webClient.put()
                    .uri(uri)
                    .addHeaders<WebClient.RequestBodyUriSpec>(httpHeaders)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono<T>()
        }
    }

    protected inline fun <reified T : Any> patchForEntity(uri: URI, payload: Any): T {
        return patchForEntity(uri, payload, null)
    }

    protected inline fun <reified T : Any> patchForEntity(uri: URI, payload: Any, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) {
            webClient.patch()
                    .uri(uri)
                    .addHeaders<WebClient.RequestBodyUriSpec>(httpHeaders)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono<T>()
        }
    }

    protected inline fun <reified T : Any> deleteForEntity(uri: URI): T {
        return deleteForEntity(uri, null)
    }

    protected inline fun <reified T : Any> deleteForEntity(uri: URI, httpHeaders: HttpHeaders?): T {
        return executeMedMetrics(uri) {
            webClient.delete().uri(uri).addHeaders<WebClient.RequestHeadersUriSpec<*>>(httpHeaders).retrieve().bodyToMono<T>()
        }
    }

    protected fun <T> executeMedMetrics(uri: URI, function: () -> Mono<T>): T {
        try {
            val startTime = System.nanoTime()
            val mono = function.invoke()
            val t = mono.block() as T
            responstid.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            responsSuccess.increment()
            return t
        } catch (e: WebClientException) {
            responsFailure.increment()
            throw e
        } catch (e: Exception) {
            responsFailure.increment()
            throw RuntimeException("Feil ved kall mot uri=$uri", e)
        }
    }

    override fun toString(): String = this::class.simpleName + " [operations=" + webClient + "]"

}
