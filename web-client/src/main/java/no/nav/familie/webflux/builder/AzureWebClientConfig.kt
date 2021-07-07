package no.nav.familie.webflux.builder

import no.nav.familie.webflux.filter.BearerTokenFilterFunction
import no.nav.familie.webflux.filter.ConsumerIdFilterFunction
import no.nav.familie.webflux.filter.InternLoggerFilterFunction
import no.nav.familie.webflux.filter.MdcValuesPropagatingFilterFunction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient


@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(ConsumerIdFilterFunction::class,
        InternLoggerFilterFunction::class,
        BearerTokenFilterFunction::class,
        NaisProxyCustomizer::class,
        NaisNoProxyCustomizer::class)
class AzureWebClientConfig {

    @Bean("azureWebClientBuilder")
    fun azureWebClientBuilder(consumerIdFilterFunction: ConsumerIdFilterFunction,
                              internLoggerFilterFunction: InternLoggerFilterFunction,
                              bearerTokenFilterFunction: BearerTokenFilterFunction,
                              iNaisProxyCustomizer: INaisProxyCustomizer): WebClient.Builder {
        val builder = WebClient.builder()
                .filter(consumerIdFilterFunction)
                .filter(bearerTokenFilterFunction)
                .filter(MdcValuesPropagatingFilterFunction())

        iNaisProxyCustomizer.customize(builder)
        return builder

    }

    @Bean("azureWebClient")
    fun azureWebClientBuilder(azureWebClientBuilder: WebClient.Builder): WebClient {
        return azureWebClientBuilder.build()
    }

}
