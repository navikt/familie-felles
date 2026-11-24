package no.nav.familie.pdl.config

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class PdlKlientPropertiesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `blankt behandlingsnummer er ugyldig`(blanktBehandlingsnummer: String) {
        val ugyldigeProperties = validator.validate(gyldigeProperties(behandlingsnummer = blanktBehandlingsnummer))
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).containsExactly("behandlingsnummer")
    }

    @Test
    fun `behandlingsnummer er valgfritt`() {
        val ugyldigeProperties = validator.validate(gyldigeProperties(behandlingsnummer = null))
        assertThat(ugyldigeProperties).isEmpty()
    }

    @Test
    fun `scope med feil format er ugyldig`() {
        val ugyldigeProperties = validator.validate(gyldigeProperties(scope = "pdl/.default"))
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("scope")
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://pdl.test", "http://pdl.test", "http://localhost:1337/rest/api/pdl"])
    fun `url med http eller https er gyldig`(url: String) {
        val ugyldigeProperties = validator.validate(gyldigeProperties(url = url))
        assertThat(ugyldigeProperties).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["ftp://pdl.test", "pdl.test", "dummy"])
    fun `url uten http eller https er ugyldig`(url: String) {
        val ugyldigeProperties = validator.validate(gyldigeProperties(url = url))
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).containsExactly("url")
    }

    @Test
    fun `negative timeouts er ugyldige`() {
        val ugyldigeProperties =
            validator.validate(
                gyldigeProperties(
                    connectTimeout = Duration.ofSeconds(-1),
                    readTimeout = Duration.ofSeconds(-1),
                ),
            )
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("connectTimeout", "readTimeout")
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 1, 999_999])
    fun `timeouts under en millisekund er ugyldige`(nanosekunder: Long) {
        val timeout = Duration.ofNanos(nanosekunder)
        val ugyldigeProperties = validator.validate(gyldigeProperties(connectTimeout = timeout, readTimeout = timeout))
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("connectTimeout", "readTimeout")
    }

    @ParameterizedTest
    @ValueSource(longs = [1, Int.MAX_VALUE.toLong()])
    fun `timeouts innenfor int-millisekundomraadet er gyldige`(millisekunder: Long) {
        val timeout = Duration.ofMillis(millisekunder)
        val ugyldigeProperties = validator.validate(gyldigeProperties(connectTimeout = timeout, readTimeout = timeout))
        assertThat(ugyldigeProperties).isEmpty()
    }

    @Test
    fun `timeouts over int-millisekundomraadet er ugyldige`() {
        val overMaksimum = Duration.ofMillis(Int.MAX_VALUE.toLong() + 1)
        val ugyldigeProperties =
            validator.validate(
                gyldigeProperties(
                    connectTimeout = overMaksimum,
                    readTimeout = overMaksimum,
                ),
            )
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("connectTimeout", "readTimeout")
    }

    @Test
    fun `gyldig https og standardtimeouts er gyldige`() {
        val properties = gyldigeProperties()

        assertThat(validator.validate(properties)).isEmpty()
        assertThat(properties.connectTimeout).isEqualTo(Duration.ofSeconds(2))
        assertThat(properties.readTimeout).isEqualTo(Duration.ofSeconds(10))
    }

    @Test
    fun `blank url og scope er ugyldige`() {
        val ugyldigeProperties = validator.validate(gyldigeProperties(url = " ", scope = " "))
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("url", "scope")
    }

    @Test
    fun `tom url er ugyldig`() {
        val ugyldigeProperties = validator.validate(gyldigeProperties(url = ""))
        assertThat(ugyldigeProperties.map { it.propertyPath.toString() }).contains("url")
    }

    private fun gyldigeProperties(
        url: String = "https://pdl.test",
        scope: String = "api://pdl.test.app/.default",
        behandlingsnummer: String? = "B123",
        connectTimeout: Duration = Duration.ofSeconds(2),
        readTimeout: Duration = Duration.ofSeconds(10),
    ) = PdlKlientProperties(url, scope, behandlingsnummer, connectTimeout, readTimeout)
}
