package no.nav.familie.felles.klient.retry

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class KlientRetryPropertiesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `standardverdier er gyldige`() {
        val properties = KlientRetryProperties()

        assertThat(properties.enabled).isTrue()
        assertThat(properties.maxRetries).isEqualTo(2L)
        assertThat(properties.delay).isEqualTo(Duration.ofMillis(100))
        assertThat(validator.validate(properties)).isEmpty()
    }

    @Test
    fun `grenseverdier er gyldige`() {
        listOf(
            KlientRetryProperties(maxRetries = 1, delay = Duration.ofMillis(1)),
            KlientRetryProperties(maxRetries = 5, delay = Duration.ofSeconds(1)),
        ).forEach { assertThat(validator.validate(it)).isEmpty() }
    }

    @Test
    fun `verdier utenfor grensene avvises`() {
        mapOf(
            KlientRetryProperties(maxRetries = 0) to "maxRetries",
            KlientRetryProperties(maxRetries = 6) to "maxRetries",
            KlientRetryProperties(delay = Duration.ZERO) to "delay",
            KlientRetryProperties(delay = Duration.ofMillis(1_001)) to "delay",
        ).forEach { (properties, felt) ->
            assertThat(validator.validate(properties).map { it.propertyPath.toString() }).containsExactly(felt)
        }
    }
}
