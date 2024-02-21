package no.nav.familie.http.config

import no.nav.familie.http.interceptor.BearerTokenClientCredentialsClientInterceptor
import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.BearerTokenOnBehalfOfClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.InternLoggerInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(
    RestTemplateBuilderBean::class,
    ConsumerIdClientInterceptor::class,
    InternLoggerInterceptor::class,
    BearerTokenClientInterceptor::class,
    BearerTokenClientCredentialsClientInterceptor::class,
    BearerTokenOnBehalfOfClientInterceptor::class,
)
class RestTemplateAzure {
    @Bean("azure")
    fun restTemplateJwtBearer(
        restClientBuilder: RestClient.Builder,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        internLoggerInterceptor: InternLoggerInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
    ): RestClient {
        return restClientBuilder
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(bearerTokenClientInterceptor)
            .requestInterceptor(MdcValuesPropagatingClientInterceptor())
            .build()
    }

    @Bean("azureClientCredential")
    fun restTemplateClientCredentialBearer(
        restClientBuilder: RestClient.Builder,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        internLoggerInterceptor: InternLoggerInterceptor,
        bearerTokenClientInterceptor: BearerTokenClientCredentialsClientInterceptor,
    ): RestClient {
        return restClientBuilder
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(bearerTokenClientInterceptor)
            .requestInterceptor(MdcValuesPropagatingClientInterceptor())
            .build()
    }

    @Bean("azureOnBehalfOf")
    fun restTemplateOnBehalfOfBearer(
        restClientBuilder: RestClient.Builder,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
        internLoggerInterceptor: InternLoggerInterceptor,
        bearerTokenClientInterceptor: BearerTokenOnBehalfOfClientInterceptor,
    ): RestClient {
        return restClientBuilder
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(bearerTokenClientInterceptor)
            .requestInterceptor(MdcValuesPropagatingClientInterceptor())
            .build()
    }
}
