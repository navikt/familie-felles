package no.nav.familie.http.config

import no.nav.familie.http.interceptor.*
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestOperations


@Suppress("SpringFacetCodeInspection")
@Configuration
@ComponentScan("no.nav.familie.http.interceptor")
@Import(RestTemplateBuilder::class)
class RestTemplateAzure {

    @Bean("azure")
    fun restTemplateJwtBearer(restTemplateBuilder: RestTemplateBuilder,
                              consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                              internLoggerInterceptor: InternLoggerInterceptor,
                              bearerTokenClientInterceptor: BearerTokenClientInterceptor): RestOperations {
        return restTemplateBuilder.interceptors(consumerIdClientInterceptor,
                                                bearerTokenClientInterceptor,
                                                MdcValuesPropagatingClientInterceptor()).build()
    }
}

