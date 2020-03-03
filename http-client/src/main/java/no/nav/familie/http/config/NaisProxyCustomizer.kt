package no.nav.familie.http.config

import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.protocol.HttpContext
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class NaisProxyCustomizer : RestTemplateCustomizer {

    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost("webproxy-nais.nav.no", 8088)
        val client: HttpClient = HttpClientBuilder.create()
                .setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {

            public override fun determineProxy(target: HttpHost,
                                               request: HttpRequest, context: HttpContext): HttpHost? {
                return if (target.hostName.contains("microsoft")) {
                    super.determineProxy(target, request, context)
                } else null
            }
        }).build()

        restTemplate.requestFactory =
                HttpComponentsClientHttpRequestFactory(client)
    }
}
