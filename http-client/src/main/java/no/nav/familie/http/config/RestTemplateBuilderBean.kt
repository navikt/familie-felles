package no.nav.familie.http.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(NaisProxyCustomizer::class)
class RestTemplateBuilderBean {

    @Bean
    @Primary
    @ConditionalOnProperty("no.nav.security.jwt.issuer.azuread.proxyurl")
    fun restTemplateBuilder(iNaisProxyCustomizer: INaisProxyCustomizer): RestTemplateBuilder {
        return RestTemplateBuilder(iNaisProxyCustomizer)
    }

    @Bean
    fun restTemplateBuilderNoProxy(): RestTemplateBuilder {
        return RestTemplateBuilder()
    }

}
