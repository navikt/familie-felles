package no.nav.familie.oppgave.config

import no.nav.familie.felles.klient.KlientMiljø.PREPROD
import no.nav.familie.felles.klient.KlientMiljø.PROD
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class OppgaveKlientPropertiesTest {
    @Test
    fun `korrekt url, scope og timeouts hentes for preprod`() {
        // Arrange
        val properties = OppgaveKlientProperties()

        // Act
        val url = properties.hentUrl(PREPROD)
        val scope = properties.hentScope(PREPROD)

        // Assert
        assertThat(url).isEqualTo("https://oppgave.dev-fss-pub.nais.io")
        assertThat(scope).isEqualTo("api://dev-fss.oppgavehandtering.oppgave/.default")
        assertThat(properties.connectTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(properties.readTimeout).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `korrekt url, scope og timeouts hentes for prod`() {
        // Arrange
        val properties = OppgaveKlientProperties()

        // Act
        val url = properties.hentUrl(PROD)
        val scope = properties.hentScope(PROD)

        // Assert
        assertThat(url).isEqualTo("https://oppgave.prod-fss-pub.nais.io")
        assertThat(scope).isEqualTo("api://prod-fss.oppgavehandtering.oppgave/.default")
        assertThat(properties.connectTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(properties.readTimeout).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `url og scope overstyrer miljø og kan brukes uten miljø`() {
        // Arrange
        val properties =
            OppgaveKlientProperties(
                url = "https://oppgave.test",
                scope = "api://dev-fss.test.oppgave/.default",
            )

        // Act
        val prodUrl = properties.hentUrl(PROD)
        val prodScope = properties.hentScope(PROD)
        val urlUtenMiljø = properties.hentUrl(null)
        val scopeUtenMiljø = properties.hentScope(null)

        // Assert
        assertThat(prodUrl).isEqualTo("https://oppgave.test")
        assertThat(prodScope).isEqualTo("api://dev-fss.test.oppgave/.default")
        assertThat(urlUtenMiljø).isEqualTo(prodUrl)
        assertThat(scopeUtenMiljø).isEqualTo(prodScope)
    }

    @Test
    fun `manglende miljø og overstyringer gir feil for url og scope`() {
        // Arrange
        val properties = OppgaveKlientProperties()

        // Act
        val urlFeil = assertThrows<IllegalStateException> { properties.hentUrl(null) }
        val scopeFeil = assertThrows<IllegalStateException> { properties.hentScope(null) }

        // Assert
        assertThat(urlFeil.message).contains("familie.felles.klient.miljo", "familie.felles.klient.oppgave.url")
        assertThat(scopeFeil.message).contains("familie.felles.klient.miljo", "familie.felles.klient.oppgave.scope")
    }
}
