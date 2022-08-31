package no.nav.familie.valutakurs.domene

import java.math.BigDecimal
import java.time.LocalDate

data class ExchangeRate(val currency: String, val exchangeRate: BigDecimal, val date: LocalDate)

fun List<ExchangeRate>.exchangeRateForCurrency(currency: String): ExchangeRate? {
    return this.firstOrNull { it.currency == currency }
}
