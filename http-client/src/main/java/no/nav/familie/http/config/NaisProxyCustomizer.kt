package no.nav.familie.http.config

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.protocol.HttpContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

interface INaisProxyCustomizer : RestTemplateCustomizer

@Component
class NaisProxyCustomizer(
    @Value("\${familie.nais.proxy.connectTimeout:15000}") val connectTimeout: Int,
    @Value("\${familie.nais.proxy.socketTimeout:15000}") val socketTimeout: Int,
    @Value("\${familie.nais.proxy.requestTimeout:15000}") val requestTimeout: Int
) : INaisProxyCustomizer {

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
                    } else null
                }
            }).build()

        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
    }
}
