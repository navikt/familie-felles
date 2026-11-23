package no.nav.familie.pdl.config

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.felles.klient.KlientProperties
import no.nav.familie.felles.klient.KlientTokenProvider
import no.nav.familie.felles.tokenklient.entraid.EntraIDClient
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import no.nav.familie.pdl.PdlException
import no.nav.familie.pdl.PdlHybridKlient
import no.nav.familie.pdl.PdlKlient
import no.nav.familie.pdl.PdlMaskinKlient
import no.nav.familie.pdl.PdlOboKlient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI

class PdlKlientAutoConfigurationTest {
    @Test
    fun `uten enabled lages alle tre klientene`() {
        val factory = testFactory()

        runner(*gyldigeProperties.filterNot { it.startsWith("familie.felles.klient.pdl.enabled") }.toTypedArray())
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys)
                    .containsExactlyInAnyOrder("pdlOboKlient", "pdlHybridKlient", "pdlMaskinKlient")
            }
    }

    @Test
    fun `false enabled gir ingen pdl beans eller krav til properties og factory`() {
        runner("familie.felles.klient.pdl.enabled=false").run { context ->
            assertThat(context.startupFailure).isNull()
            assertThat(context.getBeansOfType(PdlKlientProperties::class.java)).isEmpty()
            assertThat(context.getBeansOfType(PdlKlient::class.java)).isEmpty()
        }
    }

    @Test
    fun `enabled med properties og factory lager alle tre klientene`() {
        val factory = testFactory()

        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys)
                    .containsExactlyInAnyOrder("pdlOboKlient", "pdlHybridKlient", "pdlMaskinKlient")
                assertThat(context.getBean<PdlOboKlient>()).isNotNull()
                assertThat(context.getBean<PdlHybridKlient>()).isNotNull()
                assertThat(context.getBean<PdlMaskinKlient>()).isNotNull()
                listOf("pdlOboKlient", "pdlHybridKlient", "pdlMaskinKlient").forEach {
                    assertThat(context.beanFactory.getBeanDefinition(it).isPrimary).isFalse()
                }
                verify(exactly = 1) { factory.lagOboRestKlient("api://pdl.test.app/.default", any()) }
                verify(exactly = 1) { factory.lagHybridRestKlient("api://pdl.test.app/.default", any()) }
                verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient("api://pdl.test.app/.default") }
            }
    }

    @Test
    fun `manglende properties feiler oppstart`() {
        runner()
            .withBean(EntraIDRestClientFactory::class.java, { testFactory() })
            .run { context ->
                assertThat(context.startupFailure).isNotNull()
            }
    }

    @Test
    fun `klientene bruker faktisk http-oppsett og riktig tokenflyt`() {
        val entraIDClient = mockk<EntraIDClient>()
        val brukertoken =
            PlainJWT(
                JWTClaimsSet.Builder().claim("preferred_username", "test@nav.no").build(),
            ).serialize()
        val oboToken = "obo-token"
        val maskinToken = "maskintoken"
        every { entraIDClient.hentOboToken("api://pdl.test.app/.default", brukertoken) } returns oboToken
        every { entraIDClient.hentMaskinTilMaskinToken("api://pdl.test.app/.default") } returns maskinToken
        val provider =
            mockk<KlientTokenProvider> {
                every { hentToken() } returns brukertoken
            }

        runnerUtenProvider(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { ekteFactory(entraIDClient) })
            .withBean(KlientTokenProvider::class.java, { provider })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val query = $$"query HentPerson($ident: String!) { ping(ident: $ident) }"
                val forventetBody = """{"query":"$query","variables":{"ident":"ident"}}"""

                listOf(
                    context.getBean<PdlOboKlient>() to "Bearer $oboToken",
                    context.getBean<PdlHybridKlient>() to "Bearer $oboToken",
                    context.getBean<PdlMaskinKlient>() to "Bearer $maskinToken",
                ).forEach { (klient, authorization) ->
                    val builder = klient.restClient.mutate()
                    val server = MockRestServiceServer.bindTo(builder).build()
                    server
                        .expect(requestTo("https://pdl.test/graphql"))
                        .andExpect(method(HttpMethod.POST))
                        .andExpect(header(HttpHeaders.AUTHORIZATION, authorization))
                        .andExpect(header("Behandlingsnummer", "B123"))
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(content().json(forventetBody, true))
                        .andRespond(withSuccess("""{"data":"svar"}""", MediaType.APPLICATION_JSON))

                    val testKlient =
                        PdlMaskinKlient(URI.create("https://pdl.test"), builder.build(), klient.behandlingsnummer)
                    assertThat(testKlient.hent<String>(query, mapOf("ident" to "ident")))
                        .isEqualTo("svar")
                    server.verify()
                }

                verify(exactly = 2) { provider.hentToken() }
                verify(exactly = 2) { entraIDClient.hentOboToken("api://pdl.test.app/.default", brukertoken) }
                verify(exactly = 1) { entraIDClient.hentMaskinTilMaskinToken("api://pdl.test.app/.default") }
            }
    }

    @Test
    fun `oboklienten stopper uten brukertoken og henter ikke token`() {
        val entraIDClient = mockk<EntraIDClient>()

        runnerUtenProvider(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { ekteFactory(entraIDClient) })
            .withBean(KlientTokenProvider::class.java, { KlientTokenProvider { null } })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val klient = context.getBean<PdlOboKlient>()

                assertThatThrownBy { klient.hent<String>("query { ping }", emptyMap()) }
                    .isInstanceOf(PdlException::class.java)
                    .hasMessage("Kall mot PDL feilet")
                    .hasRootCauseMessage("Mangler brukertoken for OBO-kall mot PDL")
                verify(exactly = 0) { entraIDClient.hentOboToken(any(), any()) }
                verify(exactly = 0) { entraIDClient.hentMaskinTilMaskinToken(any()) }
            }
    }

    @Test
    fun `timeout utenfor int-millisekundomraadet feiler oppstart`() {
        assertConfigurationFails(
            *gyldigeProperties,
            "familie.felles.klient.pdl.connect-timeout=999999ns",
        )
        assertConfigurationFails(
            *gyldigeProperties,
            "familie.felles.klient.pdl.read-timeout=2147483648ms",
        )
    }

    @Test
    fun `behandlingsnummer er valgfritt og sendes til alle tre klientene`() {
        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { testFactory() })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(PdlKlient::class.java).values.map { it.behandlingsnummer })
                    .containsOnly("B123")
            }

        runner(*gyldigeProperties.filterNot { it.contains(".behandlingsnummer=") }.toTypedArray())
            .withBean(EntraIDRestClientFactory::class.java, { testFactory() })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(PdlKlient::class.java).values.map { it.behandlingsnummer })
                    .hasSize(3)
                    .containsOnlyNulls()
            }
    }

    @Test
    fun `blankt behandlingsnummer avviser oppstart`() {
        assertConfigurationFails(
            *gyldigeProperties.filterNot { it.contains(".behandlingsnummer=") }.toTypedArray(),
            "familie.felles.klient.pdl.behandlingsnummer= ",
        )
    }

    @Test
    fun `preprod bruker miljodefaults når url og scope mangler`() {
        assertMiljoDefaults(
            "preprod",
            "https://pdl-api.dev-fss-pub.nais.io",
            "api://dev-fss.pdl.pdl-api/.default",
        )
    }

    @Test
    fun `prod bruker miljodefaults når url og scope mangler`() {
        assertMiljoDefaults(
            "prod",
            "https://pdl-api.prod-fss-pub.nais.io",
            "api://prod-fss.pdl.pdl-api/.default",
        )
    }

    @Test
    fun `eksplisitt url overstyrer miljodefault mens scope beholder miljodefault`() {
        val factory = testFactory()
        runner(*miljoProperties("preprod"), "familie.felles.klient.pdl.url=https://pdl.overstyrt.test")
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val properties = context.getBean<PdlKlientProperties>()
                val miljo = context.getBean<KlientProperties>().miljo
                assertThat(properties.hentUrl(miljo)).isEqualTo("https://pdl.overstyrt.test")
                assertThat(properties.hentScope(miljo)).isEqualTo("api://dev-fss.pdl.pdl-api/.default")
                verifiserScope(factory, "api://dev-fss.pdl.pdl-api/.default")
            }
    }

    @Test
    fun `eksplisitt scope overstyrer miljodefault mens url beholder miljodefault`() {
        val factory = testFactory()
        runner(*miljoProperties("prod"), "familie.felles.klient.pdl.scope=api://override.test.app/.default")
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val properties = context.getBean<PdlKlientProperties>()
                val miljo = context.getBean<KlientProperties>().miljo
                assertThat(properties.hentUrl(miljo)).isEqualTo("https://pdl-api.prod-fss-pub.nais.io")
                assertThat(properties.hentScope(miljo)).isEqualTo("api://override.test.app/.default")
                verifiserScope(factory, "api://override.test.app/.default")
            }
    }

    @Test
    fun `eksplisitt url og scope virker uten miljo`() {
        val factory = testFactory()
        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val properties = context.getBean<PdlKlientProperties>()
                assertThat(properties.hentUrl(null)).isEqualTo("https://pdl.test")
                assertThat(properties.hentScope(null)).isEqualTo("api://pdl.test.app/.default")
                verifiserScope(factory, "api://pdl.test.app/.default")
            }
    }

    @Test
    fun `miljo under pdl gir ingen skjult fallback`() {
        assertConfigurationFails(*grunnleggendeProperties, "familie.felles.klient.pdl.miljo=prod")
    }

    @Test
    fun `mangler miljo url og scope feiler validering`() {
        assertConfigurationFails(*grunnleggendeProperties)
    }

    @Test
    fun `ukjent miljo feiler oppstart`() {
        assertConfigurationFails(*grunnleggendeProperties, "familie.felles.klient.miljo=ukjent")
    }

    @Test
    fun `en eksplisitt verdi uten miljo feiler når den andre mangler`() {
        assertConfigurationFails(*grunnleggendeProperties, "familie.felles.klient.pdl.url=https://pdl.test")
        assertConfigurationFails(
            *grunnleggendeProperties,
            "familie.felles.klient.pdl.scope=api://pdl.test.app/.default",
        )
    }

    @Test
    fun `blank url eller scope over miljodefault feiler validering`() {
        assertConfigurationFails(*miljoProperties("preprod"), "familie.felles.klient.pdl.url= ")
        assertConfigurationFails(*miljoProperties("preprod"), "familie.felles.klient.pdl.scope= ")
    }

    @Test
    fun `ugyldig url eller scope over miljodefault feiler validering`() {
        assertConfigurationFails(*miljoProperties("preprod"), "familie.felles.klient.pdl.url=http://pdl.test")
        assertConfigurationFails(*miljoProperties("preprod"), "familie.felles.klient.pdl.scope=ugyldig")
    }

    @Test
    fun `oppstart oppretter EntraID-kjeden og alle tre PDL-klientene`() {
        runner(*gyldigeProperties)
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(EntraIDClient::class.java)).hasSize(1)
                assertThat(context.getBeansOfType(EntraIDRestClientFactory::class.java)).hasSize(1)
                assertThat(context.getBeansOfType(ConsumerIdClientInterceptor::class.java)).hasSize(1)
                assertThat(context.getBeansOfType(MdcValuesPropagatingClientInterceptor::class.java)).hasSize(1)
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys)
                    .containsExactlyInAnyOrder("pdlOboKlient", "pdlHybridKlient", "pdlMaskinKlient")
            }
    }

    @Test
    fun `manglende EntraID endepunktproperties feiler oppstart`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration::class.java,
                    PdlKlientAutoConfiguration::class.java,
                ),
            ).withPropertyValues(*gyldigeProperties, "application.name=test-app")
            .run { context ->
                assertThat(context.startupFailure)
                    .hasRootCauseInstanceOf(IllegalArgumentException::class.java)
                    .hasStackTraceContaining("NAIS_TOKEN_ENDPOINT")
            }
    }

    @Test
    fun `egen oboklient erstatter bare oboklienten`() {
        val factory = testFactory()
        val egenKlient = PdlOboKlient(URI.create("https://pdl.test"), RestClient.builder().build())

        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .withBean("egenOboKlient", PdlOboKlient::class.java, { egenKlient })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBean<PdlOboKlient>()).isSameAs(egenKlient)
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys)
                    .containsExactlyInAnyOrder("egenOboKlient", "pdlHybridKlient", "pdlMaskinKlient")
                verify(exactly = 0) { factory.lagOboRestKlient(any(), any()) }
                verify(exactly = 1) { factory.lagHybridRestKlient(any(), any()) }
                verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient(any()) }
            }
    }

    @Test
    fun `egen hybridklient erstatter bare hybridklienten`() {
        val factory = testFactory()
        val egenKlient = PdlHybridKlient(URI.create("https://pdl.test"), RestClient.builder().build())

        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .withBean("egenHybridKlient", PdlHybridKlient::class.java, { egenKlient })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBean<PdlHybridKlient>()).isSameAs(egenKlient)
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys)
                    .containsExactlyInAnyOrder("pdlOboKlient", "egenHybridKlient", "pdlMaskinKlient")
                verify(exactly = 1) { factory.lagOboRestKlient(any(), any()) }
                verify(exactly = 0) { factory.lagHybridRestKlient(any(), any()) }
                verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient(any()) }
            }
    }

    @Test
    fun `egen maskinklient erstatter bare maskinklienten`() {
        val factory = testFactory()
        val egenKlient = PdlMaskinKlient(URI.create("https://pdl.test"), RestClient.builder().build())

        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .withBean("egenMaskinKlient", PdlMaskinKlient::class.java, { egenKlient })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBean<PdlMaskinKlient>()).isSameAs(egenKlient)
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys)
                    .containsExactlyInAnyOrder("pdlOboKlient", "pdlHybridKlient", "egenMaskinKlient")
                verify(exactly = 1) { factory.lagOboRestKlient(any(), any()) }
                verify(exactly = 1) { factory.lagHybridRestKlient(any(), any()) }
                verify(exactly = 0) { factory.lagMaskinTilMaskinRestKlient(any()) }
            }
    }

    @Test
    fun `obo og hybrid bruker appens tokenprovider`() {
        val factory = testFactory()
        val provider = KlientTokenProvider { "brukertoken" }

        runnerUtenProvider(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .withBean("egenProvider", KlientTokenProvider::class.java, { provider })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(KlientTokenProvider::class.java)).hasSize(1)
                verify(exactly = 1) {
                    factory.lagOboRestKlient(
                        "api://pdl.test.app/.default",
                        match { it() == "brukertoken" },
                    )
                }
                verify(exactly = 1) {
                    factory.lagHybridRestKlient(
                        "api://pdl.test.app/.default",
                        match { it() == "brukertoken" },
                    )
                }
            }
    }

    @Test
    fun `uten tokenprovider opprettes bare maskinklienten`() {
        val factory = testFactory()

        runnerUtenProvider(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(KlientTokenProvider::class.java)).isEmpty()
                assertThat(context.getBeansOfType(PdlKlient::class.java).keys).containsExactly("pdlMaskinKlient")
                verify(exactly = 0) { factory.lagOboRestKlient(any(), any()) }
                verify(exactly = 0) { factory.lagHybridRestKlient(any(), any()) }
                verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient(any()) }
            }
    }

    @Test
    fun `maskinklienten henter bare maskintoken`() {
        val entraIDClient = mockk<EntraIDClient>()
        val feil = IllegalStateException("Testfeil ved maskintokeninnhenting")
        every { entraIDClient.hentMaskinTilMaskinToken(any()) } throws feil

        runner(*gyldigeProperties)
            .withBean(EntraIDRestClientFactory::class.java, { ekteFactory(entraIDClient) })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val klient = context.getBean<PdlMaskinKlient>()
                assertThatThrownBy { klient.hent<String>("query { ping }", emptyMap()) }
                    .isInstanceOf(PdlException::class.java)
                    .hasMessage("Kall mot PDL feilet")
                    .cause()
                    .isSameAs(feil)
                verify(exactly = 1) { entraIDClient.hentMaskinTilMaskinToken("api://pdl.test.app/.default") }
                verify(exactly = 0) { entraIDClient.hentOboToken(any(), any()) }
            }
    }

    private fun runner(vararg properties: String) =
        runnerUtenProvider(*properties)
            .withBean("testTokenProvider", KlientTokenProvider::class.java, { KlientTokenProvider { null } })

    private fun runnerUtenProvider(vararg properties: String) =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PdlKlientAutoConfiguration::class.java))
            .withPropertyValues(
                "NAIS_TOKEN_ENDPOINT=https://token.test/token",
                "NAIS_TOKEN_EXCHANGE_ENDPOINT=https://token.test/exchange",
                "application.name=test-app",
                *properties,
            )

    private fun testFactory(): EntraIDRestClientFactory =
        mockk {
            every { lagOboRestKlient(any(), any()) } returns RestClient.builder().build()
            every { lagHybridRestKlient(any(), any()) } returns RestClient.builder().build()
            every { lagMaskinTilMaskinRestKlient(any()) } returns RestClient.builder().build()
        }

    private fun ekteFactory(entraIDClient: EntraIDClient): EntraIDRestClientFactory =
        EntraIDRestClientFactory(
            entraIDClient,
            ConsumerIdClientInterceptor("test-app", ""),
            MdcValuesPropagatingClientInterceptor(),
        )

    private fun verifiserScope(
        factory: EntraIDRestClientFactory,
        scope: String,
    ) {
        verify(exactly = 1) { factory.lagOboRestKlient(scope, any()) }
        verify(exactly = 1) { factory.lagHybridRestKlient(scope, any()) }
        verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient(scope) }
    }

    private fun assertMiljoDefaults(
        miljo: String,
        forventetUrl: String,
        forventetScope: String,
    ) {
        val factory = testFactory()
        runner(*miljoProperties(miljo))
            .withBean(EntraIDRestClientFactory::class.java, { factory })
            .run { context ->
                assertThat(context.startupFailure).isNull()
                val properties = context.getBean<PdlKlientProperties>()
                val klientMiljo = context.getBean<KlientProperties>().miljo
                assertThat(properties.hentUrl(klientMiljo)).isEqualTo(forventetUrl)
                assertThat(properties.hentScope(klientMiljo)).isEqualTo(forventetScope)
                verifiserScope(factory, forventetScope)
            }
    }

    private fun assertConfigurationFails(vararg properties: String) {
        runner(*properties)
            .withBean(EntraIDRestClientFactory::class.java, { testFactory() })
            .run { context -> assertThat(context.startupFailure).isNotNull() }
    }

    private fun miljoProperties(miljo: String) = grunnleggendeProperties + "familie.felles.klient.miljo=$miljo"

    private companion object {
        val grunnleggendeProperties =
            arrayOf(
                "familie.felles.klient.pdl.enabled=true",
                "familie.felles.klient.pdl.behandlingsnummer=B123",
            )
        val gyldigeProperties =
            arrayOf(
                "familie.felles.klient.pdl.enabled=true",
                "familie.felles.klient.pdl.url=https://pdl.test",
                "familie.felles.klient.pdl.scope=api://pdl.test.app/.default",
                "familie.felles.klient.pdl.behandlingsnummer=B123",
            )
    }
}
