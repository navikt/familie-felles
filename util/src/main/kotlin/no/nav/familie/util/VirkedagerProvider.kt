package no.nav.familie.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay

object VirkedagerProvider {

    private val FASTE_HELLIGDAGER = setOf(
            MonthDay.of(1, 1),
            MonthDay.of(5, 1),
            MonthDay.of(5, 17),
            MonthDay.of(12, 25),
            MonthDay.of(12, 26)
    )

    private fun beregnBevegeligeHelligdager(år: Int): Set<MonthDay> {
        val påskedag = utledPåskedag(år)
        val skjærTorsdag = påskedag.minusDays(3)
        val langfredag = påskedag.minusDays(2)
        val andrePåskedag = påskedag.plusDays(1)
        val kristiHimmelfartsdag = påskedag.plusDays(39)
        val førstePinsedag = påskedag.plusDays(49)
        val andrePinsedag = påskedag.plusDays(50)

        return setOf(MonthDay.from(påskedag),
                     MonthDay.from(skjærTorsdag),
                     MonthDay.from(langfredag),
                     MonthDay.from(andrePåskedag),
                     MonthDay.from(kristiHimmelfartsdag),
                     MonthDay.from(førstePinsedag),
                     MonthDay.from(andrePinsedag))
    }

    fun nesteVirkedag(dagensDato: LocalDate): LocalDate {
        var nesteDag = dagensDato.plusDays(1)

        while (erHelgEllerHelligdag(nesteDag)) {
            nesteDag = nesteDag.plusDays(1)
        }
        return nesteDag
    }

    fun erHelgEllerHelligdag(dato: LocalDate): Boolean {
        val helligDager = FASTE_HELLIGDAGER + beregnBevegeligeHelligdager(dato.year)

        return dato.dayOfWeek == DayOfWeek.SATURDAY
               || dato.dayOfWeek == DayOfWeek.SUNDAY
               || helligDager.contains(MonthDay.from(dato))

    }

    //Butcher-Meeus algoritm
    private fun utledPåskedag(år: Int): LocalDate {
        val a = år % 19
        val b = år / 100
        val c = år % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val n = (h + l - 7 * m + 114) / 31
        val p = (h + l - 7 * m + 114) % 31
        return LocalDate.of(år, n, p + 1)
    }
}


