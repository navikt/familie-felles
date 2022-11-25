package no.nav.familie.webflux.builder

import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.util.Timeout
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Kode for å gi nav security en restTemplateBuilder med proxy.
 * Behovet for denne forsvinner med en gang nav security begynner å bruke webflux.
 *
 */
@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(ProxyTimeout::class)
class RestTemplateBuilderConfig(private val proxyTimeout: ProxyTimeout) {

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl", matchIfMissing = true)
    fun restTemplateBuilderNoProxy(): RestTemplateBuilder {
        return RestTemplateBuilder()
            .setConnectTimeout(Duration.of(proxyTimeout.connectTimeout, ChronoUnit.MILLIS))
            .setReadTimeout(Duration.of(proxyTimeout.requestTimeout, ChronoUnit.MILLIS))
    }

    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl")
    fun restTemplateBuilderWithProxy(): RestTemplateBuilder {
        val restTemplateCustomizer =
            object : RestTemplateCustomizer {
                override fun customize(restTemplate: RestTemplate) {
                    val proxy = HttpHost("webproxy-nais.nav.no", 8088)
                    val client: HttpClient = HttpClientBuilder.create()
                        .setDefaultRequestConfig(
                            RequestConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(proxyTimeout.connectTimeout))
                                .setConnectionRequestTimeout(Timeout.ofSeconds(proxyTimeout.requestTimeout))
                                .build()
                        )
                        .setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {

                            public override fun determineProxy(
                                target: HttpHost,
                                context: HttpContext
                            ): HttpHost? {
                                return if (target.hostName.contains("microsoft")) {
                                    super.determineProxy(target, context)
                                } else {
                                    null
                                }
                            }
                        }).build()

                    restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
                }
            }

        return RestTemplateBuilder(restTemplateCustomizer)
    }
}
