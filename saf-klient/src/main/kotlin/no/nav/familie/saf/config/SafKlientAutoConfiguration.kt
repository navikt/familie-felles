package no.nav.familie.saf.config

import no.nav.familie.felles.klient.KlientProperties
import no.nav.familie.felles.klient.KlientTokenProvider
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.saf.SafOboKlient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.URI
import java.net.http.HttpClient

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "familie.felles.klient.saf",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(SafKlientProperties::class, KlientProperties::class)
@Import(EntraIDRestClientFactory::class)
class SafKlientAutoConfiguration(
    private val safKlientProperties: SafKlientProperties,
    private val fellesKlientProperties: KlientProperties,
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
) {
    @Bean
    @ConditionalOnBean(KlientTokenProvider::class)
    @ConditionalOnMissingBean(SafOboKlient::class)
    fun safOboKlient(klientTokenProvider: KlientTokenProvider): SafOboKlient =
        SafOboKlient(
            url = URI.create(safKlientProperties.hentUrl(fellesKlientProperties.miljo)),
            retry = safKlientProperties.retry,
            restClient =
                konfigurerRestClient(
                    entraIDRestClientFactory.lagOboRestKlient(safKlientProperties.hentScope(fellesKlientProperties.miljo)) {
                        klientTokenProvider.hentToken()
                            ?: throw IllegalStateException("Mangler brukertoken for OBO-kall mot Saf")
                    },
                ),
        )

    private fun konfigurerRestClient(restClient: RestClient): RestClient =
        restClient
            .mutate()
            .requestFactory(
                JdkClientHttpRequestFactory(
                    HttpClient
                        .newBuilder()
                        .connectTimeout(safKlientProperties.connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                ).apply {
                    setReadTimeout(safKlientProperties.readTimeout)
                },
            ).build()
}
