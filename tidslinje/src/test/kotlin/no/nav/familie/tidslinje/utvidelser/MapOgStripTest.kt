package no.nav.familie.tidslinje.utvidelser

import no.nav.familie.tidslinje.Null
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.TidslinjePeriode
import no.nav.familie.tidslinje.Udefinert
import no.nav.familie.tidslinje.Verdi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MapOgStripTest {
    private var lst1 = emptyList<TidslinjePeriode<Int>>()
    private var t1: Tidslinje<Int> = Tidslinje(LocalDate.now(), emptyList())

    private fun init(lst1: List<TidslinjePeriode<Int>>) {
        this.lst1 = lst1
        this.t1 = Tidslinje(LocalDate.now(), lst1)
    }

    @Test
    fun `kan mappe verdiene i en tidslinje til verdier av en annen type`() {
        init(
            listOf(
                TidslinjePeriode(5, 1, false),
                TidslinjePeriode(3, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(1, 1, false),
            ),
        )

        val tidslinje =
            t1.map {
                if (it.verdi!! > 1) {
                    Verdi(true)
                } else {
                    Verdi(false)
                }
            }

        val korrekt = listOf(true, false)

        Assertions.assertEquals(korrekt, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
    }

    @Test
    fun `kan mappe udefinert og null til ulike verdier`() {
        init(
            listOf(
                TidslinjePeriode(5, 1, false),
                TidslinjePeriode(null, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(Udefinert(), 1, false),
            ),
        )

        val tidslinje =
            t1.map {
                when (it) {
                    is Null -> Verdi(1)
                    is Udefinert -> Verdi(2)
                    else -> Verdi(3)
                }
            }

        val korrekt = listOf(3, 1, 3, 2)

        Assertions.assertEquals(korrekt, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
    }

    @Test
    fun `kan strippe vekk udefinert og null fra begynnelsen og slutten av en tidslinje`() {
        init(
            listOf(
                TidslinjePeriode(null, 1, false),
                TidslinjePeriode(5, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(Udefinert(), 1, false),
            ),
        )

        val tidslinje = t1.trim(Udefinert(), Null())

        val korrekt = listOf(5, 2)

        Assertions.assertEquals(korrekt, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
        Assertions.assertEquals(LocalDate.now().plusDays(1), tidslinje.startsTidspunkt)
    }

    @Test
    fun `kan strippe vekk TidslinjePerioderverider bestående av heltall`() {
        init(
            listOf(
                TidslinjePeriode(1, 1, false),
                TidslinjePeriode(5, 1, false),
                TidslinjePeriode(2, 1, false),
                TidslinjePeriode(1, 1, false),
            ),
        )

        val tidslinje = t1.trim(Verdi(1))

        val korrekt = listOf(5, 2)

        Assertions.assertEquals(korrekt, tidslinje.innhold.map { it.periodeVerdi.verdi }.toList())
        Assertions.assertEquals(LocalDate.now().plusDays(1), tidslinje.startsTidspunkt)
    }
}
