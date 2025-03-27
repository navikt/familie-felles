package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class TidslinjeUtvidelserTest {
    @Nested
    inner class Trim {
        val perioderMedNullFørstSistOgIMidten: List<Periode<Int?>> =
            listOf(
                Periode(null, YearMonth.of(2025, 1).atDay(1), YearMonth.of(2025, 1).atEndOfMonth()),
                Periode(2, YearMonth.of(2025, 2).atDay(1), YearMonth.of(2025, 2).atEndOfMonth()),
                Periode(3, YearMonth.of(2025, 3).atDay(1), YearMonth.of(2025, 3).atEndOfMonth()),
                Periode(null, YearMonth.of(2025, 4).atDay(1), YearMonth.of(2025, 4).atEndOfMonth()),
                Periode(2, YearMonth.of(2025, 5).atDay(1), YearMonth.of(2025, 5).atEndOfMonth()),
                Periode(null, YearMonth.of(2025, 6).atDay(1), YearMonth.of(2025, 6).atEndOfMonth()),
            )

        @Test
        fun `skal fjerne perioder med periodeVerdi lik Null() i starten og slutten av tidslinja`() {
            // Arrange
            val forventedePerioder = perioderMedNullFørstSistOgIMidten.subList(1, 5)
            val tidslinje: Tidslinje<Int?> = perioderMedNullFørstSistOgIMidten.tilTidslinje()

            // Act
            val trimmetTidslinje = tidslinje.trim(Null())

            // Assert
            assertEquals(4, trimmetTidslinje.innhold.size)
            assertEquals(forventedePerioder, trimmetTidslinje.tilPerioder())
        }

        @Nested
        inner class TrimVenstre {
            @Test
            fun `skal fjerne perioder med periodeVerdi lik Null() i starten av tidslinja`() {
                // Arrange
                val forventedePerioder = perioderMedNullFørstSistOgIMidten.subList(1, 6)
                val tidslinje: Tidslinje<Int?> = perioderMedNullFørstSistOgIMidten.tilTidslinje()

                // Act
                val trimmetTidslinje = tidslinje.trimVenstre(Null())

                // Assert
                assertEquals(5, trimmetTidslinje.innhold.size)
                assertEquals(forventedePerioder, trimmetTidslinje.tilPerioder())
            }
        }

        @Nested
        inner class TrimHøyre {
            @Test
            fun `skal fjerne perioder med periodeVerdi lik Null() i slutten av tidslinja`() {
                // Arrange
                val forventedePerioder = perioderMedNullFørstSistOgIMidten.subList(0, 5)
                val tidslinje: Tidslinje<Int?> = perioderMedNullFørstSistOgIMidten.tilTidslinje()

                // Act
                val trimmetTidslinje = tidslinje.trimHøyre(Null())

                // Assert
                assertEquals(5, trimmetTidslinje.innhold.size)
                assertEquals(forventedePerioder, trimmetTidslinje.tilPerioder())
            }
        }
    }
}
