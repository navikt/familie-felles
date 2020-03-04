package no.nav.familie.http.config

import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestOperations


@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(NaisProxyCustomizer::class)
class RestTemplateBuilderBean {

    @Bean
    fun restTemplateBuilderJwtBearer(naisProxyCustomizer: NaisProxyCustomizer): RestTemplateBuilder {
        return RestTemplateBuilder(naisProxyCustomizer)
    }
}

