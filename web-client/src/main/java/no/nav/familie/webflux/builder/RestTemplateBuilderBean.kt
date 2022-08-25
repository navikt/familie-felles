package no.nav.familie.webflux.builder

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

/**
 * Kode for å gi nav security en restTemplateBuilder med proxy.
 * Behovet for denne forsvinner med en gang nav security begynner å bruke webflux.
 *
 */
@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(NaisProxyCustomizer::class)
class RestTemplateBuilderBean(
    @Value("\${familie.nais.proxy.connectTimeout:15000}") val connectTimeout: Int,
    @Value("\${familie.nais.proxy.socketTimeout:15000}") val socketTimeout: Int,
    @Value("\${familie.nais.proxy.requestTimeout:15000}") val requestTimeout: Int
) {

    @Bean
    fun restTemplateBuilderNoProxy(): RestTemplateBuilder {
        return RestTemplateBuilder()
    }

    @Bean
    @Primary
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl")
    fun restTemplateBuilderWithProxy(): RestTemplateBuilder {
        val restTemplateCustomizer =
            object : RestTemplateCustomizer {
                override fun customize(restTemplate: RestTemplate) {
                    val proxy = HttpHost("webproxy-nais.nav.no", 8088)
                    val client: HttpClient = HttpClientBuilder.create()
                        .setDefaultRequestConfig(
                            RequestConfig.custom()
                                .setConnectTimeout(connectTimeout)
                                .setSocketTimeout(socketTimeout)
                                .setConnectionRequestTimeout(requestTimeout)
                                .build()
                        )
                        .setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {

                            public override fun determineProxy(
                                target: HttpHost,
                                request: HttpRequest,
                                context: HttpContext
                            ): HttpHost? {
                                return if (target.hostName.contains("microsoft")) {
                                    super.determineProxy(target, request, context)
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
