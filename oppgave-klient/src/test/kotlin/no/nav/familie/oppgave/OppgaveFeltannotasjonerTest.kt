package no.nav.familie.oppgave

import jakarta.validation.Validation
import no.nav.familie.oppgave.modell.FinnOppgaverRequest
import no.nav.familie.oppgave.modell.OpprettOppgaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OppgaveFeltannotasjonerTest {
    @Test
    fun `POST-felter avviser blanke eller for lange verdier og manglende UUID`() {
        // Arrange
        Validation.buildDefaultValidatorFactory().use { factory ->
            val validator = factory.validator

            // Act
            val blankeOppgavetype = validator.validateValue(OpprettOppgaveRequest::class.java, "oppgavetype", " ")
            val blanktTema = validator.validateValue(OpprettOppgaveRequest::class.java, "tema", " ")
            val langOppgavetype = validator.validateValue(OpprettOppgaveRequest::class.java, "oppgavetype", "T".repeat(256))
            val langtTema = validator.validateValue(OpprettOppgaveRequest::class.java, "tema", "B".repeat(256))
            val manglendeUuid = validator.validateValue(OpprettOppgaveRequest::class.java, "uuid", null)

            // Assert
            assertThat(blankeOppgavetype).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("oppgavetype") }
            assertThat(blanktTema).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("tema") }
            assertThat(langOppgavetype).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("oppgavetype") }
            assertThat(langtTema).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("tema") }
            assertThat(manglendeUuid).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("uuid") }
        }
    }

    @Test
    fun `søkefelt limit avviser verdier over 1000`() {
        // Arrange
        Validation.buildDefaultValidatorFactory().use { factory ->
            // Act
            val violations = factory.validator.validateValue(FinnOppgaverRequest::class.java, "limit", 1001L)

            // Assert
            assertThat(violations).anySatisfy { assertThat(it.propertyPath.toString()).isEqualTo("limit") }
        }
    }
}
