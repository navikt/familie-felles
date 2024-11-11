package no.nav.familie.webflux.builder

import no.nav.familie.webflux.filter.BearerTokenClientCredentialFilter
import no.nav.familie.webflux.filter.BearerTokenFilter
import no.nav.familie.webflux.filter.BearerTokenFilterFunction
import no.nav.familie.webflux.filter.BearerTokenOnBehalfOfFilter
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.client.WebClient

@Suppress("SpringFacetCodeInspection")
@Configuration
@Import(
    WebClientConfig::class,
    RestTemplateBuilderConfig::class,
    NaisProxyConfig::class,
    BearerTokenFilter::class,
    BearerTokenClientCredentialFilter::class,
    BearerTokenOnBehalfOfFilter::class,
)
class AzureWebClientConfig(
    private val naisProxyCustomizer: ObjectProvider<NaisProxyCustomizer>,
) {
    @Bean("azureWebClient")
    fun azureWebClient(
        @Qualifier(FAMILIE_WEB_CLIENT_BUILDER)
        webClientBuilder: WebClient.Builder,
        bearerTokenFilter: BearerTokenFilter,
    ): WebClient = buildAzureWebClient(webClientBuilder, bearerTokenFilter)

    @Bean("azureClientCredentialWebClient")
    fun azureClientCredentialWebClient(
        @Qualifier(FAMILIE_WEB_CLIENT_BUILDER)
        webClientBuilder: WebClient.Builder,
        bearerTokenFilter: BearerTokenClientCredentialFilter,
    ): WebClient = buildAzureWebClient(webClientBuilder, bearerTokenFilter)

    @Bean("azureOnBehalfOfWebClient")
    fun azureOnBehalfOfWebClient(
        @Qualifier(FAMILIE_WEB_CLIENT_BUILDER)
        webClientBuilder: WebClient.Builder,
        bearerTokenFilter: BearerTokenOnBehalfOfFilter,
    ): WebClient = buildAzureWebClient(webClientBuilder, bearerTokenFilter)

    private fun buildAzureWebClient(
        webClientBuilder: WebClient.Builder,
        bearerTokenFilter: BearerTokenFilterFunction,
    ): WebClient {
        val builder = webClientBuilder.filter(bearerTokenFilter)
        naisProxyCustomizer.ifAvailable {
            it.customize(builder)
        }
        return builder.build()
    }
}
