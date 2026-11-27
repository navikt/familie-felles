package no.nav.familie.saf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SafModellerTest {
    @Test
    fun `toString viser ikke query eller variables`() {
        // Arrange
        val query = "query"
        val variables = "variables"
        val request = SafRequest(query, mapOf("verdi" to variables))

        // Act
        val tekst = request.toString()

        // Assert
        assertThat(tekst).isEqualTo("SafRequest")
        assertThat(tekst).doesNotContain(query, variables)
    }

    @Test
    fun `toString viser ikke data eller feil`() {
        // Arrange
        val data = "data"
        val code = "code"
        val respons = SafRespons(data, listOf(SafFeil(extensions = SafFeilExtensions(code))))

        // Act
        val tekst = respons.toString()

        // Assert
        assertThat(tekst).isEqualTo("SafRespons")
        assertThat(tekst).doesNotContain(data, code)
    }
}
