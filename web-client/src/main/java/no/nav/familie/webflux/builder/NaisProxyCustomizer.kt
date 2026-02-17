package no.nav.familie.webflux.builder

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.HttpProxy
import org.eclipse.jetty.client.Origin
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.JettyClientHttpConnector
import org.springframework.http.client.reactive.JettyResourceFactory
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class NaisProxyConfig {
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl")
    @Bean
    fun naisProxyCustomizer(
        proxyTimeout: ProxyTimeout,
        jettyResourceFactory: JettyResourceFactory,
    ): NaisProxyCustomizer = NaisProxyCustomizer(proxyTimeout, jettyResourceFactory)
}

/**
 * Bruker ikke [WebClientCustomizer] + Component for å ikke få den inn automatisk i webClientBuilder
 */
class NaisProxyCustomizer(
    private val proxyTimeout: ProxyTimeout,
    private val jettyResourceFactory: JettyResourceFactory,
) {
    fun customize(webClientBuilder: WebClient.Builder) {
        class DynamicProxy(
            host: String,
            port: Int,
        ) : HttpProxy(host, port) {
            override fun matches(origin: Origin): Boolean {
                if (origin.address.host.contains("microsoft")) {
                    return true
                }
                return super.matches(origin)
            }
        }

        val proxy = DynamicProxy("webproxy-nais.nav.no", 8088)

        val httpClient = HttpClient()
        httpClient.connectTimeout = proxyTimeout.connectTimeout
        httpClient.addressResolutionTimeout = proxyTimeout.socketTimeout
        httpClient.idleTimeout = proxyTimeout.requestTimeout
        httpClient.proxyConfiguration.proxies.add(proxy)
        val connector = JettyClientHttpConnector(httpClient, jettyResourceFactory)
        webClientBuilder.clientConnector(connector)
    }
}
