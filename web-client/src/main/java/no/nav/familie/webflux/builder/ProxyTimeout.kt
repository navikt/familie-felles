package no.nav.familie.webflux.builder

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class ProxyTimeout(
    @Value("\${familie.proxy.timeout.connect:2000}") val connectTimeout: Long,
    @Value("\${familie.proxy.timeout.socket:15000}") val socketTimeout: Long,
    @Value("\${familie.proxy.timeout.request:15000}") val requestTimeout: Long,
)
