package no.nav.familie.webflux.builder

import no.nav.familie.webflux.filter.ConsumerIdFilterFunction
import no.nav.familie.webflux.filter.InternLoggerFilterFunction
import no.nav.familie.webflux.filter.MdcValuesPropagatingFilterFunction
import no.nav.familie.webflux.filter.StsBearerTokenFilterFunction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient


@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(ConsumerIdFilterFunction::class,
        InternLoggerFilterFunction::class,
        StsBearerTokenFilterFunction::class)
class StsWebClientConfig {

    @Bean("stsWebClientBuilder")
    fun stsWebClientBuilder(consumerIdFilterFunction: ConsumerIdFilterFunction,
                            internLoggerFilterFunction: InternLoggerFilterFunction,
                            stsBearerTokenFilterFunction: StsBearerTokenFilterFunction): WebClient.Builder {
        return WebClient.builder()
                .filter(consumerIdFilterFunction)
                .filter(internLoggerFilterFunction)
                .filter(stsBearerTokenFilterFunction)
                .filter(MdcValuesPropagatingFilterFunction())
    }

    @Bean("stsWebClient")
    fun azureWebClientBuilder(stsWebClientBuilder: WebClient.Builder): WebClient {
        return stsWebClientBuilder.build()
    }
}

