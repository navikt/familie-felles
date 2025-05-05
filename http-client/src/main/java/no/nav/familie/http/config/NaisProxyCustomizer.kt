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
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

interface INaisProxyCustomizer : RestTemplateCustomizer

@Component
class NaisProxyCustomizer(
    @Value("\${familie.nais.proxy.connectTimeout:15000}") val connectTimeout: Long,
    @Value("\${familie.nais.proxy.socketTimeout:15000}") val socketTimeout: Long,
    @Value("\${familie.nais.proxy.requestTimeout:15000}") val requestTimeout: Long,
) : INaisProxyCustomizer {
    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost("webproxy-nais.nav.no", 8088)
        val client: HttpClient =
            HttpClientBuilder
                .create()
                .setDefaultRequestConfig(
                    RequestConfig
                        .custom()
                        // ConnectionConfig.Builder.setConnectTimeout(Timeout
                        .setConnectTimeout(Timeout.ofSeconds(connectTimeout))
                        .setConnectionRequestTimeout(Timeout.ofSeconds(requestTimeout))
                        .build(),
                ).setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder
                        .create()
                        .setDefaultSocketConfig(
                            SocketConfig
                                .custom()
                                .setSoTimeout(Timeout.ofMilliseconds(socketTimeout))
                                .build(),
                        ).build(),
                ).setRoutePlanner(
                    object : DefaultProxyRoutePlanner(proxy) {
                        public override fun determineProxy(
                            target: HttpHost,
                            context: HttpContext,
                        ): HttpHost? =
                            if (target.hostName.contains("microsoft")) {
                                super.determineProxy(target, context)
                            } else {
                                null
                            }
                    },
                ).build()

        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
    }
}
