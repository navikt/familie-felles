package no.nav.familie.http.config

import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.util.Timeout
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory

@Configuration
class NaisProxyCustomizer(
    @Value("\${familie.nais.proxy.connectTimeout:15000}") val connectTimeout: Long,
    @Value("\${familie.nais.proxy.socketTimeout:15000}") val socketTimeout: Long,
    @Value("\${familie.nais.proxy.requestTimeout:15000}") val requestTimeout: Long,
) {
    @Bean
    fun buildHttpClientWithProxy(): HttpClient {
        val proxyHost = HttpHost("webproxy-nais.nav.no", 8088)
        val connectionConfig =
            org.apache.hc.client5.http.config.ConnectionConfig
                .custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeout))
                .build()

        val socketConfig =
            SocketConfig
                .custom()
                .setSoTimeout(Timeout.ofMilliseconds(socketTimeout))
                .build()

        val connectionManager =
            PoolingHttpClientConnectionManagerBuilder
                .create()
                .setDefaultConnectionConfig(connectionConfig)
                .setDefaultSocketConfig(socketConfig)
                .build()

        val requestConfig =
            RequestConfig
                .custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(requestTimeout))
                .build()

        return HttpClientBuilder
            .create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setRoutePlanner(
                object : DefaultProxyRoutePlanner(proxyHost) {
                    override fun determineProxy(
                        target: HttpHost,
                        context: HttpContext,
                    ): HttpHost? = if (target.hostName.contains("microsoft")) super.determineProxy(target, context) else null
                },
            ).build()
    }

    @Bean
    fun restTemplateBuilder(): RestTemplateBuilder {
        val client = buildHttpClientWithProxy()
        return RestTemplateBuilder()
            .requestFactory { HttpComponentsClientHttpRequestFactory(client) }
    }
}
