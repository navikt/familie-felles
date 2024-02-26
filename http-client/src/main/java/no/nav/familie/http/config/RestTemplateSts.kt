package no.nav.familie.http.config

import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(RestTemplateBuilderBean::class, StsBearerTokenClientInterceptor::class, ConsumerIdClientInterceptor::class)
class RestTemplateSts {
    @Bean("sts")
    fun restTemplateSts(
        restClientBuilder: RestClient.Builder,
        stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    ): RestClient {
        return restClientBuilder
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(stsBearerTokenClientInterceptor)
            .requestInterceptor(MdcValuesPropagatingClientInterceptor())
            .build()
    }
}
