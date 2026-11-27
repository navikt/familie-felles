package no.nav.familie.oppgave.config

import no.nav.familie.felles.klient.KlientProperties
import no.nav.familie.felles.klient.KlientTokenProvider
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.oppgave.OppgaveHybridKlient
import no.nav.familie.oppgave.OppgaveMaskinKlient
import no.nav.familie.oppgave.OppgaveOboKlient
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
    prefix = "familie.felles.klient.oppgave",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(OppgaveKlientProperties::class, KlientProperties::class)
@Import(EntraIDRestClientFactory::class)
class OppgaveKlientAutoConfiguration(
    private val oppgaveKlientProperties: OppgaveKlientProperties,
    private val fellesKlientProperties: KlientProperties,
    private val entraIDRestClientFactory: EntraIDRestClientFactory,
) {
    @Bean
    @ConditionalOnBean(KlientTokenProvider::class)
    @ConditionalOnMissingBean(OppgaveOboKlient::class)
    fun oppgaveOboKlient(klientTokenProvider: KlientTokenProvider): OppgaveOboKlient =
        OppgaveOboKlient(
            url = url(),
            restClient =
                konfigurerRestClient(
                    entraIDRestClientFactory.lagOboRestKlient(scope()) {
                        klientTokenProvider.hentToken()?.takeIf(String::isNotBlank)
                            ?: throw IllegalStateException("Mangler brukertoken for OBO-kall mot Oppgave")
                    },
                ),
        )

    @Bean
    @ConditionalOnBean(KlientTokenProvider::class)
    @ConditionalOnMissingBean(OppgaveHybridKlient::class)
    fun oppgaveHybridKlient(klientTokenProvider: KlientTokenProvider): OppgaveHybridKlient =
        OppgaveHybridKlient(
            url = url(),
            restClient =
                konfigurerRestClient(
                    entraIDRestClientFactory.lagHybridRestKlient(scope()) { klientTokenProvider.hentToken() },
                ),
        )

    @Bean
    @ConditionalOnMissingBean(OppgaveMaskinKlient::class)
    fun oppgaveMaskinKlient(): OppgaveMaskinKlient =
        OppgaveMaskinKlient(
            url = url(),
            restClient = konfigurerRestClient(entraIDRestClientFactory.lagMaskinTilMaskinRestKlient(scope())),
        )

    private fun url(): URI = URI.create(oppgaveKlientProperties.hentUrl(fellesKlientProperties.miljo))

    private fun scope(): String = oppgaveKlientProperties.hentScope(fellesKlientProperties.miljo)

    private fun konfigurerRestClient(restClient: RestClient): RestClient =
        restClient
            .mutate()
            .requestFactory(
                JdkClientHttpRequestFactory(
                    HttpClient
                        .newBuilder()
                        .connectTimeout(oppgaveKlientProperties.connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build(),
                ).apply {
                    setReadTimeout(oppgaveKlientProperties.readTimeout)
                },
            ).build()
}
