package no.nav.familie.sikkerhet.context

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant

/**
 * Tester oppførselen til [TokenContextHolder] avhengig av hvilke konfigurasjoner som importeres:
 *
 * - **Ingen**: [TokenContextValidationAutoConfiguration] feiler konteksten ved oppstart med [TokenContextConfigurationException].
 * - **Bare [FamilieFellesSpringSecurityKonfigurasjon]**: én bean — konteksten starter uten feil.
 * - **Bare [FamilieFellesNavTokenSupportKonfigurasjon]**: én bean — konteksten starter uten feil.
 * - **Begge**: [TokenContextValidationAutoConfiguration] feiler konteksten ved oppstart med [TokenContextConfigurationException].
 * - **Med NAIS-miljøvariabler**: issuerNameMapping bygges automatisk fra `AZURE_OPENID_CONFIG_ISSUER` og `TOKEN_X_ISSUER`.
 */
internal class TokenContextHolderKonfigurasjonsTest {
    private val applicationContextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TokenContextValidationAutoConfiguration::class.java))

    @Configuration
    open class IngenKonfigurasjon

    @Configuration
    @Import(FamilieFellesSpringSecurityKonfigurasjon::class)
    open class BareSpringSecurityKonfigurasjon

    @Configuration
    @Import(FamilieFellesNavTokenSupportKonfigurasjon::class)
    open class BareNavTokenSupportKonfigurasjon

    @Configuration
    @Import(FamilieFellesSpringSecurityKonfigurasjon::class, FamilieFellesNavTokenSupportKonfigurasjon::class)
    open class BeggeKonfigurasjoner

    @Test
    fun `ingen konfigurasjon - konteksten feiler ved oppstart med TokenContextConfigurationException`() {
        applicationContextRunner
            .withUserConfiguration(IngenKonfigurasjon::class.java)
            .run { ctx ->
                Assertions.assertThat(ctx).hasFailed()
                Assertions
                    .assertThat(ctx.startupFailure)
                    .rootCause()
                    .isInstanceOf(TokenContextConfigurationException::class.java)
                    .hasMessageContaining("Ingen TokenContext er konfigurert")
            }
    }

    @Test
    fun `bare SpringSecurity - én bean - konteksten starter uten feil`() {
        applicationContextRunner
            .withUserConfiguration(BareSpringSecurityKonfigurasjon::class.java)
            .run { ctx ->
                Assertions.assertThat(ctx).hasNotFailed()
                Assertions.assertThat(ctx).hasSingleBean(SpringSecurityTokenContext::class.java)
                Assertions.assertThat(ctx).doesNotHaveBean(NavTokenSupportTokenContext::class.java)
            }
    }

    @Test
    fun `bare NavTokenSupport - én bean - konteksten starter uten feil`() {
        applicationContextRunner
            .withUserConfiguration(BareNavTokenSupportKonfigurasjon::class.java)
            .run { ctx ->
                Assertions.assertThat(ctx).hasNotFailed()
                Assertions.assertThat(ctx).hasSingleBean(NavTokenSupportTokenContext::class.java)
                Assertions.assertThat(ctx).doesNotHaveBean(SpringSecurityTokenContext::class.java)
            }
    }

    @Test
    fun `begge konfigurasjoner - kontekstoppstart feiler med for mange TokenContext-beans`() {
        applicationContextRunner
            .withUserConfiguration(BeggeKonfigurasjoner::class.java)
            .run { ctx ->
                Assertions.assertThat(ctx).hasFailed()
                Assertions
                    .assertThat(ctx.startupFailure)
                    .rootCause()
                    .isInstanceOf(TokenContextConfigurationException::class.java)
                    .hasMessageContaining("Flere TokenContext'er er konfigurert")
            }
    }

    @Test
    fun `miljøvariabler gir automatisk issuerNameMapping`() {
        val azureIssuerUrl = "https://login.microsoftonline.com/tenant/v2.0"
        val tokenxIssuerUrl = "https://tokenx.nav.no"

        applicationContextRunner
            .withUserConfiguration(BareSpringSecurityKonfigurasjon::class.java)
            .withPropertyValues(
                "AZURE_OPENID_CONFIG_ISSUER=$azureIssuerUrl",
                "TOKEN_X_ISSUER=$tokenxIssuerUrl",
            ).run { ctx ->
                Assertions.assertThat(ctx).hasNotFailed()

                mockJwt(azureIssuerUrl)
                Assertions.assertThat(TokenContextHolder.hasTokenFor("azuread")).isTrue()
                Assertions.assertThat(TokenContextHolder.hasTokenFor("tokenx")).isFalse()

                mockJwt(tokenxIssuerUrl)
                Assertions.assertThat(TokenContextHolder.hasTokenFor("tokenx")).isTrue()
                Assertions.assertThat(TokenContextHolder.hasTokenFor("azuread")).isFalse()

                SecurityContextHolder.clearContext()
            }
    }
}

private fun mockJwt(issuerUrl: String) {
    val jwt =
        Jwt(
            "dummy.jwt.token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            mapOf("alg" to "RS256"),
            mapOf("iss" to issuerUrl),
        )
    SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
}
