package no.nav.familie.http.config

import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestOperations

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(RestTemplateBuilderBean::class, StsBearerTokenClientInterceptor::class, ConsumerIdClientInterceptor::class)
class RestTemplateSts {
    @Bean("sts")
    fun restTemplateSts(
        restTemplateBuilder: RestTemplateBuilder,
        stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    ): RestOperations =
        restTemplateBuilder
            .additionalInterceptors(
                consumerIdClientInterceptor,
                stsBearerTokenClientInterceptor,
                MdcValuesPropagatingClientInterceptor(),
            ).build()
}
