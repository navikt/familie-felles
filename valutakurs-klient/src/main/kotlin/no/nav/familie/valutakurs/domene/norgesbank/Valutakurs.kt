package no.nav.familie.valutakurs.domene.norgesbank

import java.math.BigDecimal
import java.time.LocalDate

data class Valutakurs(
    val valuta: String,
    val kurs: BigDecimal,
    val kursDato: LocalDate,
)
