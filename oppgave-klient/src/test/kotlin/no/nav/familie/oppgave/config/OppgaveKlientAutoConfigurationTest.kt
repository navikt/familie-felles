package no.nav.familie.oppgave.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.felles.klient.KlientMiljø.PREPROD
import no.nav.familie.felles.klient.KlientProperties
import no.nav.familie.felles.klient.KlientTokenProvider
import no.nav.familie.felles.tokenklient.entraid.EntraIDRestClientFactory
import no.nav.familie.oppgave.OppgaveHybridKlient
import no.nav.familie.oppgave.OppgaveKlient
import no.nav.familie.oppgave.OppgaveMaskinKlient
import no.nav.familie.oppgave.OppgaveOboKlient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClient
import java.net.URI

class OppgaveKlientAutoConfigurationTest {
    private val factory = mockFactory()

    @Test
    fun `alle klient-varianter opprettes ved oppstart med riktig scope og token callbacks når det finnes en KlientTokenProvider`() {
        // Arrange
        val oboTokenSupplier = slot<() -> String>()
        val hybridTokenSupplier = slot<() -> String?>()
        every { factory.lagOboRestKlient(any(), capture(oboTokenSupplier)) } returns restClient
        every { factory.lagHybridRestKlient(any(), capture(hybridTokenSupplier)) } returns restClient
        val token = "token-fra-provider"

        // Act
        applicationContextRunner()
            .withBean(KlientTokenProvider::class.java, { KlientTokenProvider { token } })
            .run { context ->
                // Assert
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBeansOfType(OppgaveKlient::class.java)).isEmpty()
                assertThat(context.getBeansOfType(OppgaveOboKlient::class.java).keys).containsExactly("oppgaveOboKlient")
                assertThat(context.getBeansOfType(OppgaveHybridKlient::class.java).keys).containsExactly("oppgaveHybridKlient")
                assertThat(context.getBeansOfType(OppgaveMaskinKlient::class.java).keys).containsExactly("oppgaveMaskinKlient")
                listOf("oppgaveOboKlient", "oppgaveHybridKlient", "oppgaveMaskinKlient").forEach { name ->
                    assertThat(context.beanFactory.getBeanDefinition(name).isPrimary).isFalse()
                }
                verify(exactly = 1) { factory.lagOboRestKlient(SCOPE, any()) }
                verify(exactly = 1) { factory.lagHybridRestKlient(SCOPE, any()) }
                verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient(SCOPE) }
                assertThat(oboTokenSupplier.captured.invoke()).isEqualTo(token)
                assertThat(hybridTokenSupplier.captured.invoke()).isEqualTo(token)
            }
    }

    @Test
    fun `kun maskinklient opprettes ved oppstart når det ikke finnes en KlientTokenProvider`() {
        // Act
        applicationContextRunner().run { context ->
            // Assert
            assertThat(context.startupFailure).isNull()
            assertThat(context.getBeansOfType(OppgaveKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveOboKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveHybridKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveMaskinKlient::class.java).keys).containsExactly("oppgaveMaskinKlient")
            verify(exactly = 0) { factory.lagOboRestKlient(any(), any()) }
            verify(exactly = 0) { factory.lagHybridRestKlient(any(), any()) }
            verify(exactly = 1) { factory.lagMaskinTilMaskinRestKlient(SCOPE) }
        }
    }

    @Test
    fun `konsument injiserer alle klientvarianter uten qualifier`() {
        // Arrange
        val runner =
            applicationContextRunner()
                .withBean(KlientTokenProvider::class.java, { KlientTokenProvider { "token" } })
                .withUserConfiguration(OppgaveKlientKonsumentKonfigurasjon::class.java)

        // Act
        runner.run { context ->
            // Assert
            assertThat(context.startupFailure).isNull()
            val konsument = context.getBean(OppgaveKlientKonsument::class.java)
            assertThat(context.getBeansOfType(OppgaveKlient::class.java)).isEmpty()
            assertThat(konsument.oboKlient).isSameAs(context.getBean(OppgaveOboKlient::class.java))
            assertThat(konsument.hybridKlient).isSameAs(context.getBean(OppgaveHybridKlient::class.java))
            assertThat(konsument.maskinKlient).isSameAs(context.getBean(OppgaveMaskinKlient::class.java))
            assertThat(OppgaveOboKlient::class.java.superclass).isEqualTo(Any::class.java)
            assertThat(OppgaveHybridKlient::class.java.superclass).isEqualTo(Any::class.java)
            assertThat(OppgaveMaskinKlient::class.java.superclass).isEqualTo(Any::class.java)
        }
    }

    @Test
    fun `ingen klienter oppretts når oppgave-klient er disabled i properties`() {
        // Arrange
        val disabledRunner =
            ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OppgaveKlientAutoConfiguration::class.java))
                .withPropertyValues("familie.felles.klient.oppgave.enabled=false")

        // Act
        disabledRunner.run { context ->
            // Assert
            assertThat(context.startupFailure).isNull()
            assertThat(context.getBeansOfType(OppgaveKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveOboKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveHybridKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveMaskinKlient::class.java)).isEmpty()
            assertThat(context.getBeansOfType(OppgaveKlientProperties::class.java)).isEmpty()
        }
    }

    @Test
    fun `manuell opprettelse av klient-beans erstatter automatisk opprettelse`() {
        // Arrange
        val oboKlient = OppgaveOboKlient(url, restClient)
        val hybridKlient = OppgaveHybridKlient(url, restClient)
        val maskinKlient = OppgaveMaskinKlient(url, restClient)

        // Act
        applicationContextRunner()
            .withBean(OppgaveOboKlient::class.java, { oboKlient })
            .withBean(OppgaveHybridKlient::class.java, { hybridKlient })
            .withBean(OppgaveMaskinKlient::class.java, { maskinKlient })
            .run { context ->
                // Assert
                assertThat(context.startupFailure).isNull()
                assertThat(context.getBean<OppgaveOboKlient>()).isSameAs(oboKlient)
                assertThat(context.getBean<OppgaveHybridKlient>()).isSameAs(hybridKlient)
                assertThat(context.getBean<OppgaveMaskinKlient>()).isSameAs(maskinKlient)
                assertThat(context.getBeansOfType(OppgaveKlient::class.java)).isEmpty()
                verify(exactly = 0) { factory.lagOboRestKlient(any(), any()) }
                verify(exactly = 0) { factory.lagHybridRestKlient(any(), any()) }
                verify(exactly = 0) { factory.lagMaskinTilMaskinRestKlient(any()) }
            }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [" "])
    fun `oboKlient skal avvise kall uten token`(token: String?) {
        // Arrange
        val tokenSupplier = slot<() -> String>()
        every { factory.lagOboRestKlient(any(), capture(tokenSupplier)) } returns restClient
        val autoConfiguration =
            OppgaveKlientAutoConfiguration(
                OppgaveKlientProperties(),
                KlientProperties(PREPROD),
                factory,
            )

        // Act
        autoConfiguration.oppgaveOboKlient { token }
        val feil = assertThrows<IllegalStateException> { tokenSupplier.captured.invoke() }

        // Assert
        assertThat(feil.message).isEqualTo("Mangler brukertoken for OBO-kall mot Oppgave")
    }

    private fun applicationContextRunner() =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OppgaveKlientAutoConfiguration::class.java))
            .withPropertyValues(
                "familie.felles.klient.miljo=PREPROD",
                "NAIS_TOKEN_ENDPOINT=https://token.test/token",
                "NAIS_TOKEN_EXCHANGE_ENDPOINT=https://token.test/exchange",
                "application.name=test-app",
            ).withBean(EntraIDRestClientFactory::class.java, { factory })

    private fun mockFactory() =
        mockk<EntraIDRestClientFactory> {
            every { lagOboRestKlient(any(), any()) } returns restClient
            every { lagHybridRestKlient(any(), any()) } returns restClient
            every { lagMaskinTilMaskinRestKlient(any()) } returns restClient
        }

    private companion object {
        const val SCOPE = "api://dev-fss.oppgavehandtering.oppgave/.default"
        val url: URI = URI.create("https://oppgave.test")
        val restClient: RestClient = RestClient.builder().build()
    }
}

@Configuration(proxyBeanMethods = false)
@Import(OppgaveKlientKonsument::class)
private class OppgaveKlientKonsumentKonfigurasjon

private class OppgaveKlientKonsument(
    val oboKlient: OppgaveOboKlient,
    val hybridKlient: OppgaveHybridKlient,
    val maskinKlient: OppgaveMaskinKlient,
)
