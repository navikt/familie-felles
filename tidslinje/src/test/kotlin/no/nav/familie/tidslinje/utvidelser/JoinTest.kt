package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.PeriodeVerdi
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.mnd
import no.nav.familie.tidslinje.somMnd
import no.nav.familie.tidslinje.tilPeriodeVerdi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Hver map skrives som `nøkkel:tidslinje` skilt med semikolon, for eksempel `a:12;b:-3`.
 *
 * leftJoin og outerJoin testes bare med nøkler som finnes på begge sider. Når en nøkkel mangler på én side,
 * kombineres den andre siden i dag med en tom tidslinje som starter i år 0, og kombinatoren kalles fra år 0.
 */
class JoinTest {
    private fun String.map(): Map<String, Tidslinje<Int>> =
        if (isEmpty()) {
            emptyMap()
        } else {
            split(";").associate { it.substringBefore(":") to it.substringAfter(":").mnd() }
        }

    private fun Map<String, Tidslinje<Int>>.vis(): String = entries.joinToString(";") { "${it.key}:${it.value.somMnd()}" }

    private fun sum(vararg verdier: Int?): Int? = if (verdier.all { it == null }) null else verdier.sumOf { it ?: 0 }

    @ParameterizedTest(name = "{0} join {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            // Bare nøkler som finnes på begge sider
            "a:1;b:2     | a:2;c:1     | a:3",
            "a:1         | b:1         | ''",
            "''          | a:1         | ''",
            "a:1         | a:-2        | a:12",
            "a:1>        | a:2         | a:31>",
        ],
    )
    fun `join kombinerer tidslinjene med samme nøkkel`(
        venstre: String,
        høyre: String,
        forventet: String,
    ) {
        assertEquals(forventet, venstre.map().join(høyre.map()) { a, b -> sum(a, b) }.vis())
    }

    @ParameterizedTest(name = "{0} joinIkkeNull {1} = {2}")
    @CsvSource(
        delimiter = '|',
        value = [
            "a:1;b:2     | a:2;b:-2    | a:3;b:nn",
            "a:1         | b:1         | ''",
            "a:1n        | a:22        | a:3n",
            "a:1>        | a:-2>       | a:n3>",
        ],
    )
    fun `joinIkkeNull kombinerer bare der begge har verdi`(
        venstre: String,
        høyre: String,
        forventet: String,
    ) {
        assertEquals(forventet, venstre.map().joinIkkeNull(høyre.map()) { a, b -> a + b }.vis())
    }

    @Test
    fun `leftJoin beholder nøklene fra venstre side`() {
        val resultat = "a:1;b:2".map().leftJoin("a:2;b:-2;c:1".map()) { a, b -> sum(a, b) }

        assertEquals("a:3;b:22", resultat.vis())
    }

    @Test
    fun `outerJoin beholder nøklene fra begge sider`() {
        val resultat = "a:1;b:2".map().outerJoin("b:-2;a:-2".map()) { a, b -> sum(a, b) }

        assertEquals("a:12;b:22", resultat.vis())
    }

    @Test
    fun `outerJoin med tre map kombinerer tidslinjene med samme nøkkel`() {
        val resultat = "a:1;b:1".map().outerJoin("a:-1;b:1".map(), "a:--1;b:1".map()) { a, b, c -> sum(a, b, c) }

        assertEquals("a:111;b:3", resultat.vis())
    }

    private fun sum(
        a: PeriodeVerdi<Int>,
        b: PeriodeVerdi<Int>,
    ): PeriodeVerdi<Int> = sum(a.verdi, b.verdi).tilPeriodeVerdi()

    @Test
    fun `join av liste kombinerer hver tidslinje med samme tidslinje`() {
        val resultat = listOf("1".mnd(), "-1".mnd()).join("2".mnd(), ::sum)

        assertEquals(listOf("3", "21"), resultat.map { it.somMnd() })
    }

    @Test
    fun `join av to lister kombinerer tidslinjene parvis`() {
        val resultat = listOf("1".mnd(), "-1".mnd()).join(listOf("2".mnd(), "1".mnd()), ::sum)

        assertEquals(listOf("3", "11"), resultat.map { it.somMnd() })
    }

    @Test
    fun `join av to lister med ulik lengde gir feil`() {
        assertThrows<IllegalArgumentException> { listOf("1".mnd()).join(emptyList<Tidslinje<Int>>(), ::sum) }
    }
}
