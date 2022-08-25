package no.nav.familie.webflux.builder

import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.HttpProxy
import org.eclipse.jetty.client.Origin
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Primary
import org.springframework.http.client.reactive.JettyClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

interface INaisProxyCustomizer : WebClientCustomizer

@Component
class NaisNoProxyCustomizer : INaisProxyCustomizer {

    override fun customize(webClientBuilder: WebClient.Builder?) {
        // Ingen proxy
    }
}

@Component
@Primary
@ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl")
class NaisProxyCustomizer : INaisProxyCustomizer {

    override fun customize(webClientBuilder: WebClient.Builder) {
        class DynamicProxy(host: String, port: Int) : HttpProxy(host, port) {

            override fun matches(origin: Origin): Boolean {
                if (origin.address.host.contains("microsoft")) {
                    return true
                }
                return super.matches(origin)
            }
        }

        val proxy = DynamicProxy("webproxy-nais.nav.no", 8088)

        val httpClient = HttpClient()
        httpClient.proxyConfiguration.proxies.add(proxy)
        val connector = JettyClientHttpConnector(httpClient)
        webClientBuilder.clientConnector(connector)
    }
}
