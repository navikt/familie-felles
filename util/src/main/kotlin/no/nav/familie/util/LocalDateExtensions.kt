package no.nav.familie.util

import java.time.LocalDate
import java.time.YearMonth


fun LocalDate.toYearMonth() = YearMonth.of(this.year, this.month)
