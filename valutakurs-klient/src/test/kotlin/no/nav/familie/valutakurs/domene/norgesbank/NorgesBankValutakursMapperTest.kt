package no.nav.familie.valutakurs.domene.norgesbank

import no.nav.familie.valutakurs.domene.norgesbank.Frekvens.MÅNEDLIG
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens.VIRKEDAG
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursMapper.tilValutakurs
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateAttributes
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateDate
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateKey
import no.nav.familie.valutakurs.domene.sdmx.SDMXExchangeRateValue
import no.nav.familie.valutakurs.exception.NorgesBankValutakursMappingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate

class NorgesBankValutakursMapperTest {
    @Nested
    inner class TilValutakurs {
        @Test
        fun `Skal returnere korrekt valutakurs for gyldig data`() {
            // Arrange
            val valuta = "PLN"
            val frekvens = VIRKEDAG
            val kursDato = LocalDate.of(2023, 1, 1)
            val kurs = BigDecimal("10.0000")
            val data = mockNorgesBankValutakursData(valuta, frekvens, kursDato, kurs)

            // Act
            val valutakurs = data.tilValutakurs(valuta, frekvens, kursDato)

            // Assert
            assertEquals(valuta, valutakurs.valuta)
            assertEquals(BigDecimal("10.0000"), valutakurs.kurs)
            assertEquals(kursDato, valutakurs.kursDato)
        }

        @Test
        fun `Skal kaste feil dersom valuta er ulik forventet valuta`() {
            // Arrange
            val valuta = "EUR"
            val forventetValuta = "PLN"
            val data = mockNorgesBankValutakursData(valuta = valuta)

            // Act & Assert
            val ugyldigDataException =
                assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
                    data.tilValutakurs(forventetValuta, VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Forventet valuta $forventetValuta men fikk $valuta.",
                ugyldigDataException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom frekvens er ulik forventet frekvens`() {
            // Arrange
            val frekvens = MÅNEDLIG
            val forventetFrekvens = VIRKEDAG
            val data = mockNorgesBankValutakursData(frekvens = frekvens)

            // Act & Assert
            val ugyldigDataException =
                assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
                    data.tilValutakurs("PLN", forventetFrekvens, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Forventet frekvens $forventetFrekvens men fikk $frekvens.",
                ugyldigDataException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom kursdato er ulik forventet kursdato`() {
            // Arrange
            val kursDato = LocalDate.of(2023, 2, 1)
            val forventetKursdato = LocalDate.of(2023, 1, 1)
            val data = mockNorgesBankValutakursData(kursDato = kursDato)

            // Act & Assert
            val ugyldigDataException =
                assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
                    data.tilValutakurs("PLN", VIRKEDAG, forventetKursdato)
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Forventet kursdato $forventetKursdato men fikk $kursDato.",
                ugyldigDataException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom kurs er kalkulert`() {
            // Arrange
            val data = mockNorgesBankValutakursData(calculated = "true")

            // Act & Assert
            val ugyldigDataException =
                assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Valutakurs er kalkulert og vi forventer observert verdi.",
                ugyldigDataException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom innsamlingstidspunkt ikke er C`() {
            // Arrange
            val data = mockNorgesBankValutakursData(collection = "X")

            // Act & Assert
            val ugyldigDataException =
                assertThrows<NorgesBankValutakursMappingException.UgyldigData> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Forventer at innsamlingstidspunkt er 'C' men fikk 'X'.",
                ugyldigDataException.message,
            )
        }

        @Test
        fun `skal finne rett kurs ved å dele kurs på 10^UNIT_MULT`() {
            // Arrange
            val data = mockNorgesBankValutakursData(kurs = BigDecimal("100.0000"), unitMult = "2")

            // Act
            val valutakurs = data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))

            // Assert
            assertEquals(BigDecimal("1.0000"), valutakurs.kurs)
        }

        @Test
        fun `skal kaste feil dersom BASE_CUR mangler`() {
            // Arrange
            val observations = mockNorgesBankValutakursData().dataSet.series.observations
            val attributes = mockNorgesBankValutakursData().dataSet.series.attributes
            val seriesKeyes = listOf(SDMXExchangeRateKey("FREQ", "B"))
            val data =
                NorgesBankValutakursData(
                    NorgesBankValutakursDataSet(
                        series =
                            NorgesBankValutakursSeries(
                                seriesKeys = seriesKeyes,
                                attributes = attributes,
                                observations = observations,
                            ),
                    ),
                )

            // Act & Assert
            val manglerFeltException =
                assertThrows<NorgesBankValutakursMappingException.ManglerFelt> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Mangler informasjon om BASE_CUR",
                manglerFeltException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom FREQ mangler`() {
            // Arrange
            val observations = mockNorgesBankValutakursData().dataSet.series.observations
            val attributes = mockNorgesBankValutakursData().dataSet.series.attributes
            val seriesKeyes = listOf(SDMXExchangeRateKey("BASE_CUR", "PLN"))
            val data =
                NorgesBankValutakursData(
                    NorgesBankValutakursDataSet(
                        series =
                            NorgesBankValutakursSeries(
                                seriesKeys = seriesKeyes,
                                attributes = attributes,
                                observations = observations,
                            ),
                    ),
                )

            // Act & Assert
            val manglerFeltException =
                assertThrows<NorgesBankValutakursMappingException.ManglerFelt> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Mangler informasjon om FREQ",
                manglerFeltException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom UNIT_MULT mangler`() {
            // Arrange
            val observations = mockNorgesBankValutakursData().dataSet.series.observations
            val attributes =
                listOf(
                    SDMXExchangeRateAttributes("COLLECTION", "C"),
                    SDMXExchangeRateAttributes("CALCULATED", "false"),
                )
            val seriesKeyes = mockNorgesBankValutakursData().dataSet.series.seriesKeys
            val data =
                NorgesBankValutakursData(
                    NorgesBankValutakursDataSet(
                        series =
                            NorgesBankValutakursSeries(
                                seriesKeys = seriesKeyes,
                                attributes = attributes,
                                observations = observations,
                            ),
                    ),
                )

            // Act & Assert
            val manglerFeltException =
                assertThrows<NorgesBankValutakursMappingException.ManglerFelt> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Mangler informasjon om UNIT_MULT",
                manglerFeltException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom COLLECTION mangler`() {
            // Arrange
            val observations = mockNorgesBankValutakursData().dataSet.series.observations
            val attributes =
                listOf(
                    SDMXExchangeRateAttributes("UNIT_MULT", "0"),
                    SDMXExchangeRateAttributes("CALCULATED", "false"),
                )
            val seriesKeyes = mockNorgesBankValutakursData().dataSet.series.seriesKeys
            val data =
                NorgesBankValutakursData(
                    NorgesBankValutakursDataSet(
                        series =
                            NorgesBankValutakursSeries(
                                seriesKeys = seriesKeyes,
                                attributes = attributes,
                                observations = observations,
                            ),
                    ),
                )

            // Act & Assert
            val manglerFeltException =
                assertThrows<NorgesBankValutakursMappingException.ManglerFelt> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Mangler informasjon om COLLECTION",
                manglerFeltException.message,
            )
        }

        @Test
        fun `skal kaste feil dersom CALCULATED mangler`() {
            // Arrange
            val observations = mockNorgesBankValutakursData().dataSet.series.observations
            val attributes =
                listOf(
                    SDMXExchangeRateAttributes("UNIT_MULT", "0"),
                    SDMXExchangeRateAttributes("COLLECTION", "C"),
                )
            val seriesKeyes = mockNorgesBankValutakursData().dataSet.series.seriesKeys
            val data =
                NorgesBankValutakursData(
                    NorgesBankValutakursDataSet(
                        series =
                            NorgesBankValutakursSeries(
                                seriesKeys = seriesKeyes,
                                attributes = attributes,
                                observations = observations,
                            ),
                    ),
                )

            // Act & Assert
            val manglerFeltException =
                assertThrows<NorgesBankValutakursMappingException.ManglerFelt> {
                    data.tilValutakurs("PLN", VIRKEDAG, LocalDate.of(2023, 1, 1))
                }
            assertEquals(
                "Feil ved mapping av valutakursdata fra Norges Bank. Mangler informasjon om CALCULATED",
                manglerFeltException.message,
            )
        }
    }

    private fun mockNorgesBankValutakursData(
        valuta: String = "PLN",
        frekvens: Frekvens = VIRKEDAG,
        kursDato: LocalDate = LocalDate.of(2023, 1, 1),
        kurs: BigDecimal = BigDecimal("10.0000"),
        unitMult: String = "0",
        calculated: String = "false",
        collection: String = "C",
    ): NorgesBankValutakursData {
        val seriesKeys =
            listOf(
                SDMXExchangeRateKey("BASE_CUR", valuta),
                SDMXExchangeRateKey("FREQ", frekvens.verdi),
            )
        val attributes =
            listOf(
                SDMXExchangeRateAttributes("UNIT_MULT", unitMult),
                SDMXExchangeRateAttributes("CALCULATED", calculated),
                SDMXExchangeRateAttributes("COLLECTION", collection),
            )
        val observations =
            SDMXExchangeRate(
                SDMXExchangeRateDate(kursDato.toString()),
                SDMXExchangeRateValue(kurs),
            )
        val series = NorgesBankValutakursSeries(seriesKeys, attributes, observations)
        val dataSet = NorgesBankValutakursDataSet(series)
        return NorgesBankValutakursData(dataSet)
    }
}
