package no.nav.familie.http.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(NaisProxyCustomizer::class)
class RestTemplateBuilderBean {
    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl")
    fun restTemplateBuilder(naisProxyCustomizer: NaisProxyCustomizer): RestTemplateBuilder =
        naisProxyCustomizer.customize(RestTemplateBuilder())

    /**
     * Denne bønnnen initialiseres hvis proxy-url ikke finnes. Hvis proxy-url finnnes vil bønnen over initialiseres og
     * denne det ikke med mindre proxyen har verdien "umulig verdi", som den aldri skal ha.
     */
    @Bean
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl", matchIfMissing = true, havingValue = "Umulig verdi")
    fun restTemplateBuilderNoProxy(): RestTemplateBuilder = RestTemplateBuilder()
}
