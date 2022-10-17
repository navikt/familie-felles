package no.nav.familie.webflux.builder

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class ProxyTimeout(
    @Value("\${familie.proxy.connectTimeout:2000}") val connectTimeout: Long,
    @Value("\${familie.proxy.socketTimeout:15000}") val socketTimeout: Long,
    @Value("\${familie.proxy.requestTimeout:15000}") val requestTimeout: Long
)
