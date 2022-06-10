package no.nav.familie.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VirkedagerProviderTest {
    val skjærTorsdag2021 = LocalDate.of(2021, 4, 1)
    val skjærTorsdag2022 = LocalDate.of(2022, 4, 14)

    @Test
    fun `Hent virkedag allmenlig måndag`() {
        val allmenligMåndag = LocalDate.of(2020, 10, 26)
        assertEquals(VirkedagerProvider.nesteVirkedag(allmenligMåndag), allmenligMåndag.plusDays(1))
    }

    @Test
    fun `Hent virkedag allmenlig fredag`() {
        val allmenligFredag = LocalDate.of(2020, 10, 30)
        assertEquals(VirkedagerProvider.nesteVirkedag(allmenligFredag), allmenligFredag.plusDays(3))
    }

    @Test
    fun `Hent virkedag skjærtorsdag 2021`() {
        assertEquals(VirkedagerProvider.nesteVirkedag(skjærTorsdag2021), skjærTorsdag2021.plusDays(5))
    }

    @Test
    fun `Hent virkedag skjærtorsdag 2022`() {
        assertEquals(VirkedagerProvider.nesteVirkedag(skjærTorsdag2022), skjærTorsdag2022.plusDays(5))
    }
}
