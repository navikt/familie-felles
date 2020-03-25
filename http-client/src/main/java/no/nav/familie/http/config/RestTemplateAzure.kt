package no.nav.familie.http.config

import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.InternLoggerInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestOperations


@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(RestTemplateBuilderBean::class,
        ConsumerIdClientInterceptor::class,
        InternLoggerInterceptor::class,
        BearerTokenClientInterceptor::class)
class RestTemplateAzure {

    @Bean("azure")
    fun restTemplateJwtBearer(restTemplateBuilder: RestTemplateBuilder,
                              consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                              internLoggerInterceptor: InternLoggerInterceptor,
                              bearerTokenClientInterceptor: BearerTokenClientInterceptor): RestOperations {
        return restTemplateBuilder.additionalInterceptors(consumerIdClientInterceptor,
                                                          bearerTokenClientInterceptor,
                                                          MdcValuesPropagatingClientInterceptor()).build()
    }
}

