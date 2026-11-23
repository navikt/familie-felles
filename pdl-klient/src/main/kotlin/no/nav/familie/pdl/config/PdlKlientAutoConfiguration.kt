package no.nav.familie.pdl.config

import no.nav.familie.felles.klient.KlientProperties
import no.nav.familie.felles.klient.KlientTokenProvider
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.pdl.PdlHybridKlient
import no.nav.familie.pdl.PdlMaskinKlient
import no.nav.familie.pdl.PdlOboKlient
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.URI

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "familie.felles.klient.pdl",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(PdlKlientProperties::class, KlientProperties::class)
@Import(EntraIDRestClientFactory::class)
class PdlKlientAutoConfiguration(
    private val pdlKlientProperties: PdlKlientProperties,
    private val fellesKlientProperties: KlientProperties,
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
) {
    @Bean
    @ConditionalOnBean(KlientTokenProvider::class)
    @ConditionalOnMissingBean(PdlOboKlient::class)
    fun pdlOboKlient(klientTokenProvider: KlientTokenProvider): PdlOboKlient =
        PdlOboKlient(
            url = url(),
            behandlingsnummer = pdlKlientProperties.behandlingsnummer,
            restClient =
                konfigurerRestClient(
                    entraIDRestClientFactory.lagOboRestKlient(scope()) {
                        klientTokenProvider.hentToken()
                            ?: throw IllegalStateException("Mangler brukertoken for OBO-kall mot PDL")
                    },
                ),
        )

    @Bean
    @ConditionalOnBean(KlientTokenProvider::class)
    @ConditionalOnMissingBean(PdlHybridKlient::class)
    fun pdlHybridKlient(klientTokenProvider: KlientTokenProvider): PdlHybridKlient =
        PdlHybridKlient(
            url = url(),
            behandlingsnummer = pdlKlientProperties.behandlingsnummer,
            restClient =
                konfigurerRestClient(
                    entraIDRestClientFactory.lagHybridRestKlient(scope()) { klientTokenProvider.hentToken() },
                ),
        )

    @Bean
    @ConditionalOnMissingBean(PdlMaskinKlient::class)
    fun pdlMaskinKlient(): PdlMaskinKlient =
        PdlMaskinKlient(
            url = url(),
            behandlingsnummer = pdlKlientProperties.behandlingsnummer,
            restClient = konfigurerRestClient(entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope())),
        )

    private fun url(): URI = URI.create(pdlKlientProperties.hentUrl(fellesKlientProperties.miljo))

    private fun scope(): String = pdlKlientProperties.hentScope(fellesKlientProperties.miljo)

    private fun konfigurerRestClient(restClient: RestClient): RestClient =
        restClient
            .mutate()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(pdlKlientProperties.connectTimeout)
                    setReadTimeout(pdlKlientProperties.readTimeout)
                },
            ).build()
}
