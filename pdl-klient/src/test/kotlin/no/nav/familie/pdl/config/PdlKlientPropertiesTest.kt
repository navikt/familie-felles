package no.nav.familie.pdl.config

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class PdlKlientPropertiesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `blankt behandlingsnummer er ugyldig`() {
        listOf("", " ").forEach { blank ->
            val ugyldig = validator.validate(gyldigeProperties(behandlingsnummer = blank))
            assertThat(ugyldig.map { it.propertyPath.toString() }).containsExactly("behandlingsnummer")
        }
    }

    @Test
    fun `behandlingsnummer er valgfritt`() {
        assertThat(validator.validate(gyldigeProperties(behandlingsnummer = null))).isEmpty()
    }

    @Test
    fun `scope med feil format er ugyldig`() {
        val properties = gyldigeProperties(scope = "pdl/.default")

        assertThat(validator.validate(properties).map { it.propertyPath.toString() })
            .contains("scope")
    }

    @Test
    fun `url uten https er ugyldig`() {
        val properties = gyldigeProperties(url = "http://pdl.test")

        assertThat(validator.validate(properties).map { it.propertyPath.toString() })
            .contains("url")
    }

    @Test
    fun `negative timeouts er ugyldige`() {
        val properties =
            gyldigeProperties(
                connectTimeout = Duration.ofSeconds(-1),
                readTimeout = Duration.ofSeconds(-1),
            )

        assertThat(validator.validate(properties).map { it.propertyPath.toString() })
            .contains("connectTimeout", "readTimeout")
    }

    @Test
    fun `timeouts under en millisekund er ugyldige`() {
        listOf(Duration.ZERO, Duration.ofNanos(1), Duration.ofNanos(999_999)).forEach { timeout ->
            val properties = gyldigeProperties(connectTimeout = timeout, readTimeout = timeout)

            assertThat(validator.validate(properties).map { it.propertyPath.toString() })
                .contains("connectTimeout", "readTimeout")
        }
    }

    @Test
    fun `timeouts innenfor int-millisekundomraadet er gyldige`() {
        val maksimum = Duration.ofMillis(Int.MAX_VALUE.toLong())
        listOf(Duration.ofMillis(1), maksimum).forEach { timeout ->
            val properties = gyldigeProperties(connectTimeout = timeout, readTimeout = timeout)

            assertThat(validator.validate(properties)).isEmpty()
        }
    }

    @Test
    fun `timeouts over int-millisekundomraadet er ugyldige`() {
        val overMaksimum = Duration.ofMillis(Int.MAX_VALUE.toLong() + 1)
        val properties = gyldigeProperties(connectTimeout = overMaksimum, readTimeout = overMaksimum)

        assertThat(validator.validate(properties).map { it.propertyPath.toString() })
            .contains("connectTimeout", "readTimeout")
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
        val properties = gyldigeProperties(url = " ", scope = " ")

        assertThat(validator.validate(properties).map { it.propertyPath.toString() })
            .contains("url", "scope")
    }

    @Test
    fun `tom url er ugyldig`() {
        val properties = gyldigeProperties(url = "")

        assertThat(validator.validate(properties).map { it.propertyPath.toString() })
            .contains("url")
    }

    private fun gyldigeProperties(
        url: String = "https://pdl.test",
        scope: String = "api://pdl.test.app/.default",
        behandlingsnummer: String? = "B123",
        connectTimeout: Duration = Duration.ofSeconds(2),
        readTimeout: Duration = Duration.ofSeconds(10),
    ) = PdlKlientProperties(url, scope, behandlingsnummer, connectTimeout, readTimeout)
}
