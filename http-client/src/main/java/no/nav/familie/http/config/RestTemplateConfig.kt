package no.nav.familie.http.config

import no.nav.familie.http.interceptor.BearerTokenClientInterceptor
import no.nav.familie.http.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.http.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.http.interceptor.StsBearerTokenClientInterceptor
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.DefaultProxyRoutePlanner
import org.apache.http.protocol.HttpContext
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate


@Configuration
class RestTemplateConfig {

    @Bean("azure")
    fun restTemplateJwtBearer(consumerIdClientInterceptor: ConsumerIdClientInterceptor,
                              bearerTokenClientInterceptor: BearerTokenClientInterceptor): RestOperations {

        return RestTemplateBuilder()
                .additionalCustomizers(NaisProxyCustomizer())
                .interceptors(consumerIdClientInterceptor,
                              bearerTokenClientInterceptor,
                              MdcValuesPropagatingClientInterceptor())
                .build()
    }

    @Bean("sts")
    fun restTemplateSts(stsBearerTokenClientInterceptor: StsBearerTokenClientInterceptor,
                        consumerIdClientInterceptor: ConsumerIdClientInterceptor): RestOperations {

        return RestTemplateBuilder()
                .interceptors(consumerIdClientInterceptor,
                              stsBearerTokenClientInterceptor,
                              MdcValuesPropagatingClientInterceptor())
                .build()
    }

}

class NaisProxyCustomizer : RestTemplateCustomizer {
    override fun customize(restTemplate: RestTemplate) {
        val proxy = HttpHost("webproxy-nais.nav.no", 8088)
        val client: HttpClient = HttpClientBuilder.create().setRoutePlanner(object : DefaultProxyRoutePlanner(proxy) {

            public override fun determineProxy(target: HttpHost,
                                               request: HttpRequest, context: HttpContext): HttpHost? {
                return if (target.hostName.contains("microsoft")) {
                    super.determineProxy(target, request, context)
                } else null
            }
        }).build()

        restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(client)
    }
}
