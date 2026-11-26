package no.nav.familie.saf.config

import jakarta.validation.Validation
import no.nav.familie.felles.klient.KlientMiljø
import no.nav.familie.felles.klient.retry.KlientRetryProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.util.stream.Stream

class SafKlientPropertiesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `url og scope kan overstyres`() {
        // Arrange
        val properties = gyldigeProperties()

        // Act
        val urlUtenMiljø = properties.hentUrl(null)
        val scopeUtenMiljø = properties.hentScope(null)
        val urlMedProdMiljø = properties.hentUrl(KlientMiljø.PROD)
        val scopeMedProdMiljø = properties.hentScope(KlientMiljø.PROD)
        val valideringsfeil = validator.validate(properties)

        // Assert
        assertThat(urlUtenMiljø).isEqualTo("https://saf.test")
        assertThat(scopeUtenMiljø).isEqualTo("api://saf.test.app/.default")
        assertThat(urlMedProdMiljø).isEqualTo("https://saf.test")
        assertThat(scopeMedProdMiljø).isEqualTo("api://saf.test.app/.default")
        assertThat(valideringsfeil).isEmpty()
    }

    @Test
    fun `scope med feil format er ugyldig`() {
        // Arrange
        val properties = gyldigeProperties(scope = "saf/.default")

        // Act
        val ugyldigeProperties = validator.validate(properties)

        // Assert
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("scope")
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://saf.test", "http://saf.test", "http://localhost:1337/rest/api/saf"])
    fun `url med http eller https er gyldig`(url: String) {
        // Arrange
        val properties = gyldigeProperties(url = url)

        // Act
        val ugyldigeProperties = validator.validate(properties)

        // Assert
        assertThat(ugyldigeProperties).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["ftp://saf.test", "saf.test", "dummy"])
    fun `url uten http eller https er ugyldig`(url: String) {
        // Arrange
        val properties = gyldigeProperties(url = url)

        // Act
        val ugyldigeProperties = validator.validate(properties)

        // Assert
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).containsExactly("url")
    }

    @ParameterizedTest
    @MethodSource("timeoutGrenser")
    fun `timeoutgrenser valideres`(
        timeout: Duration,
        gyldig: Boolean,
    ) {
        // Arrange
        val properties = gyldigeProperties(connectTimeout = timeout, readTimeout = timeout)

        // Act
        val valideringsfeil = validator.validate(properties)

        // Assert
        if (gyldig) {
            assertThat(valideringsfeil).isEmpty()
        } else {
            assertThat(valideringsfeil.map { it.propertyPath.toString() }).contains("connectTimeout", "readTimeout")
        }
    }

    @Test
    fun `gyldig https og standardtimeouts er gyldige`() {
        // Arrange
        val properties = gyldigeProperties()

        // Act
        val valideringsfeil = validator.validate(properties)

        // Assert
        assertThat(valideringsfeil).isEmpty()
        assertThat(properties.connectTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(properties.readTimeout).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `retry valideres som del av saf-propertyene`() {
        // Arrange
        val properties = gyldigeProperties()
        val ugyldigeProperties = gyldigeProperties(retry = KlientRetryProperties(maxRetries = 6))

        // Act
        val valideringsfeil = validator.validate(properties)
        val retryValideringsfeil = validator.validate(ugyldigeProperties)

        // Assert
        assertThat(valideringsfeil).isEmpty()
        assertThat(retryValideringsfeil.map { it.propertyPath.toString() }).containsExactly("retry.maxRetries")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `tom eller blank url og blank scope er ugyldige`(url: String) {
        // Arrange
        val properties = gyldigeProperties(url = url, scope = " ")

        // Act
        val ugyldigeProperties = validator.validate(properties)

        // Assert
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("url", "scope")
    }

    private fun gyldigeProperties(
        url: String = "https://saf.test",
        scope: String = "api://saf.test.app/.default",
        connectTimeout: Duration = Duration.ofSeconds(2),
        readTimeout: Duration = Duration.ofSeconds(10),
        retry: KlientRetryProperties = KlientRetryProperties(),
    ) = SafKlientProperties(url, scope, connectTimeout, readTimeout, retry)

    companion object {
        @JvmStatic
        fun timeoutGrenser() =
            Stream.of(
                Arguments.of(Duration.ofSeconds(-1), false),
                Arguments.of(Duration.ofNanos(0), false),
                Arguments.of(Duration.ofNanos(1), false),
                Arguments.of(Duration.ofNanos(999_999), false),
                Arguments.of(Duration.ofMillis(1), true),
                Arguments.of(Duration.ofMillis(Int.MAX_VALUE.toLong()), true),
                Arguments.of(Duration.ofMillis(Int.MAX_VALUE.toLong() + 1), false),
            )
    }
}
