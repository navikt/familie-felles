package no.nav.familie.http.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(NaisProxyCustomizer::class)
class RestTemplateBuilderBean {

    @Bean
    fun restTemplateBuilder(iNaisProxyCustomizer: INaisProxyCustomizer): RestTemplateBuilder {
        return RestTemplateBuilder(iNaisProxyCustomizer)
    }
}

