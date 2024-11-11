package no.nav.familie.webflux.builder

import no.nav.familie.webflux.filter.StsBearerTokenFilter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(
    WebClientConfig::class,
    StsBearerTokenFilter::class,
)
class StsWebClientConfig {
    @Bean("stsWebClient")
    fun stsWebClient(
        @Qualifier(FAMILIE_WEB_CLIENT_BUILDER)
        webClientBuilder: WebClient.Builder,
        stsBearerTokenFilter: StsBearerTokenFilter,
    ): WebClient =
        webClientBuilder
            .filter(stsBearerTokenFilter)
            .build()
}
