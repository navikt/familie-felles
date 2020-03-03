package no.nav.familie.http.config

import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestOperations


@Suppress("SpringFacetCodeInspection")
@Configuration
@ComponentScan("no.nav.familie.http")
class RestTemplateConfig {

    @Bean("azure")
    fun restTemplateBuilderJwtBearer(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                                     bearerTokenClientInterceptor: BearerTokenClientInterceptor,
                                     naisProxyCustomizer: NaisProxyCustomizer): RestTemplateBuilder {

        return RestTemplateBuilder()
                .additionalCustomizers(naisProxyCustomizer)
                .interceptors(consumerIdClientInterceptor,
                              bearerTokenClientInterceptor,
                              MdcValuesPropagatingClientInterceptor())
    }

    @Bean("azure")
    fun restTemplateJwtBearer(@Qualifier("azure") restTemplateBuilder: RestTemplateBuilder): RestOperations {
        return restTemplateBuilder.build()
    }

    @Bean("sts")
    fun restTemplateBuilderSts(stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
                               consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestTemplateBuilder? {

        return RestTemplateBuilder()
                .interceptors(consumerIdClientInterceptor,
                              stsBearerTokenClientInterceptor,
                              MdcValuesPropagatingClientInterceptor())
    }

    @Bean("sts")
    fun restTemplateSts(@Qualifier("sts") restTemplateBuilder: RestTemplateBuilder): RestOperations {
        return restTemplateBuilder.build()
    }

}

