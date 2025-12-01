package no.nav.familie.valutakurs.domene

import java.math.BigDecimal
import java.time.LocalDate

data class Valutakurs(
    val valuta: String,
    val kurs: BigDecimal,
    val kursDato: LocalDate,
)

fun List<Valutakurs>.exchangeRateForCurrency(currency: String): Valutakurs? = this.firstOrNull { it.valuta == currency }
