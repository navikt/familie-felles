package no.nav.familie.valutakurs.domene.norgesbank

import no.nav.familie.valutakurs.exception.NorgesBankValutakursMappingException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.pow

object NorgesBankValutakursMapper {
    @Throws(NorgesBankValutakursMappingException::class)
    fun NorgesBankValutakursData.tilValutakurs(
        valuta: String,
        frekvens: Frekvens,
        kursDato: LocalDate,
    ): Valutakurs {
        this.valider(valuta, frekvens, kursDato)
        return Valutakurs(valuta = valuta, kurs = this.tilKalkulertKurs(), kursDato = kursDato)
    }

    private fun NorgesBankValutakursData.valider(
        forventetValuta: String,
        forventetFrekvens: Frekvens,
        forventetKursDato: LocalDate,
    ) {
        val valutaData = this.dataSet.series
        valutaData.validerValuta(forventetValuta)
        valutaData.validerFrekvens(forventetFrekvens)
        valutaData.validerKursDato(forventetKursDato)
        valutaData.validerErKalkulertValutakurs()
        valutaData.validerInnsamlingstidspunkt()
    }

    fun NorgesBankValutakursSeries.hentSeriesKey(id: String): String =
        this.seriesKeys.singleOrNull { it.id == id }?.value
            ?: throw NorgesBankValutakursMappingException.ManglerFelt("Mangler informasjon om $id")

    fun NorgesBankValutakursSeries.hentAttribute(id: String): String =
        this.attributes.singleOrNull { it.id == id }?.value
            ?: throw NorgesBankValutakursMappingException.ManglerFelt("Mangler informasjon om $id")

    fun NorgesBankValutakursSeries.hentKurs(): BigDecimal = observations.sdmxExchangeRateValue.value

    fun NorgesBankValutakursSeries.hentKursDato(): LocalDate =
        try {
            LocalDate.parse(observations.date.value)
        } catch (e: DateTimeParseException) {
            throw NorgesBankValutakursMappingException.UgyldigData("Respons inneholder ugyldig datoformat.", e)
        }

    private fun NorgesBankValutakursSeries.validerValuta(forventetValuta: String) {
        val valuta = hentSeriesKey("BASE_CUR")

        if (forventetValuta != valuta) {
            throw NorgesBankValutakursMappingException.UgyldigData("Forventet valuta $forventetValuta men fikk $valuta.")
        }
    }

    private fun NorgesBankValutakursSeries.validerFrekvens(forventetFrekvens: Frekvens) {
        val frekvens: Frekvens = Frekvens.fraVerdi(hentSeriesKey("FREQ"))

        if (forventetFrekvens != frekvens) {
            throw NorgesBankValutakursMappingException.UgyldigData("Forventet frekvens $forventetFrekvens men fikk $frekvens.")
        }
    }

    private fun NorgesBankValutakursSeries.validerErKalkulertValutakurs() {
        val kalkulertVerdi: Boolean = hentAttribute("CALCULATED").toBoolean()

        if (kalkulertVerdi) {
            throw NorgesBankValutakursMappingException.UgyldigData(
                "Valutakurs er kalkulert og vi forventer observert verdi.",
            )
        }
    }

    private fun NorgesBankValutakursSeries.validerInnsamlingstidspunkt() {
        val innsamlingstidspunkt = hentAttribute("COLLECTION")
        if (innsamlingstidspunkt !=
            "C"
        ) {
            throw NorgesBankValutakursMappingException.UgyldigData(
                "Forventer at innsamlingstidspunkt er 'C' men fikk '$innsamlingstidspunkt'.",
            )
        }
    }

    private fun NorgesBankValutakursSeries.validerKursDato(forventetDato: LocalDate) {
        val kursDato: LocalDate = hentKursDato()
        if (!forventetDato.isEqual(kursDato)) {
            throw NorgesBankValutakursMappingException.UgyldigData("Forventet kursdato $forventetDato men fikk $kursDato.")
        }
    }

/**
     * Norges Bank leverer ikke alltid valutakurs på lik enhet, men leverer i noen tilfeller enhetsverdi multiplisert med 10, 100 eller 1000.
     * Vi må derfor hente ut enhets-multiplikatoren med id *UNIT_MULT* og bruke denne for å kalkulere enhetsverdien.
     * Kursen kalkuleres ved (kurs / 10^UNIT_MULT). Altså vil det bli henholdsvis kurs/1, kurs/10, kurs/100 osv basert på verdien til *UNIT_MULT*.
     * Eksempelvis leveres kursen for DKK som verdien av 100 DKK og ikke 1 DKK. I dette tilfellet er *UNIT_MULT* satt til 2, og vi finner enhetsverdien ved ta kurs/100.
     * Runder av kursen til 4 desimaler da det er dette vi tidligere fikk direkte fra ECB.
     */
    private fun NorgesBankValutakursData.tilKalkulertKurs(): BigDecimal {
        val enhetMultiplikator: Double =
            this.dataSet.series
                .hentAttribute("UNIT_MULT")
                .toDouble()

        val kurs: BigDecimal = this.dataSet.series.hentKurs()

        return kurs.divide(BigDecimal.valueOf(10.0.pow(enhetMultiplikator)), 4, RoundingMode.HALF_UP)
    }
}
