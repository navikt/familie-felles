package no.nav.familie.valutakurs

import no.nav.familie.valutakurs.config.SDMXValutakursRestKlientConfig
import no.nav.familie.valutakurs.domene.ecb.ECBValutakursData
import no.nav.familie.valutakurs.domene.ecb.exchangeRatesForCurrency
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursData
import no.nav.familie.valutakurs.domene.norgesbank.NorgesBankValutakursMapper.tilValutakurs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValutakursParsingTest {
    val ecbXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <message:GenericData xmlns:message="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message" xmlns:common="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/common" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:generic="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic" xsi:schemaLocation="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message https://sdw-wsrest.ecb.europa.eu:443/vocabulary/sdmx/2_1/SDMXMessage.xsd http://www.sdmx.org/resources/sdmxml/schemas/v2_1/common https://sdw-wsrest.ecb.europa.eu:443/vocabulary/sdmx/2_1/SDMXCommon.xsd http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic https://sdw-wsrest.ecb.europa.eu:443/vocabulary/sdmx/2_1/SDMXDataGeneric.xsd">
            <message:Header>
                <message:ID>b155910b-633b-4e04-8556-5f279ff01dc7</message:ID>
                <message:Test>false</message:Test>
                <message:Prepared>2022-08-15T13:33:02.354+02:00</message:Prepared>
                <message:Sender id="ECB"/>
                <message:Structure structureID="ECB_EXR1" dimensionAtObservation="TIME_PERIOD">
                    <common:Structure>
                        <URN>urn:sdmx:org.sdmx.infomodel.datastructure.DataStructure=ECB:ECB_EXR1(1.0)</URN>
                    </common:Structure>
                </message:Structure>
            </message:Header>
            <message:DataSet action="Replace" validFromDate="2022-08-15T13:33:02.354+02:00" structureRef="ECB_EXR1">
                <generic:Series>
                    <generic:SeriesKey>
                        <generic:Value id="FREQ" value="D"/>
                        <generic:Value id="CURRENCY" value="NOK"/>
                        <generic:Value id="CURRENCY_DENOM" value="EUR"/>
                        <generic:Value id="EXR_TYPE" value="SP00"/>
                        <generic:Value id="EXR_SUFFIX" value="A"/>
                    </generic:SeriesKey>
                    <generic:Attributes>
                        <generic:Value id="COLLECTION" value="A"/>
                        <generic:Value id="UNIT" value="NOK"/>
                        <generic:Value id="DECIMALS" value="4"/>
                        <generic:Value id="TITLE" value="Norwegian krone/Euro"/>
                        <generic:Value id="TITLE_COMPL" value="ECB reference exchange rate, Norwegian krone/Euro, 2:15 pm (C.E.T.)"/>
                        <generic:Value id="UNIT_MULT" value="0"/>
                        <generic:Value id="TIME_FORMAT" value="P1D"/>
                        <generic:Value id="SOURCE_AGENCY" value="4F0"/>
                    </generic:Attributes>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-28"/>
                        <generic:ObsValue value="10.337"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-29"/>
                        <generic:ObsValue value="10.3065"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                </generic:Series>
                <generic:Series>
                    <generic:SeriesKey>
                        <generic:Value id="FREQ" value="D"/>
                        <generic:Value id="CURRENCY" value="SEK"/>
                        <generic:Value id="CURRENCY_DENOM" value="EUR"/>
                        <generic:Value id="EXR_TYPE" value="SP00"/>
                        <generic:Value id="EXR_SUFFIX" value="A"/>
                    </generic:SeriesKey>
                    <generic:Attributes>
                        <generic:Value id="TITLE" value="Swedish krona/Euro"/>
                        <generic:Value id="COLLECTION" value="A"/>
                        <generic:Value id="UNIT" value="SEK"/>
                        <generic:Value id="DECIMALS" value="4"/>
                        <generic:Value id="TITLE_COMPL" value="ECB reference exchange rate, Swedish krona/Euro, 2:15 pm (C.E.T.)"/>
                        <generic:Value id="UNIT_MULT" value="0"/>
                        <generic:Value id="TIME_FORMAT" value="P1D"/>
                        <generic:Value id="SOURCE_AGENCY" value="4F0"/>
                    </generic:Attributes>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-28"/>
                        <generic:ObsValue value="10.6543"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                    <generic:Obs>
                        <generic:ObsDimension value="2022-06-29"/>
                        <generic:ObsValue value="10.6848"/>
                        <generic:Attributes>
                            <generic:Value id="OBS_STATUS" value="A"/>
                            <generic:Value id="OBS_CONF" value="F"/>
                        </generic:Attributes>
                    </generic:Obs>
                </generic:Series>
            </message:DataSet>
        </message:GenericData>
        """.trimIndent()

    @Nested
    inner class ECBValutakursDataParsingTest {
        @Test
        fun `Test at xml parses som forventet`() {
            // Arrange
            val mapper = SDMXValutakursRestKlientConfig().xmlMapper()

            // Act
            val ecbValutakursData = mapper.readValue(ecbXml, ECBValutakursData::class.java)

            // Assert
            assertEquals(ecbValutakursData.sdmxExchangeRatesDataSet.sdmxExchangeRatesForCurrencies.size, 2)

            val nokExchangeRates = ecbValutakursData.exchangeRatesForCurrency("NOK")
            val sekExchangeRates = ecbValutakursData.exchangeRatesForCurrency("SEK")
            assertEquals(2, nokExchangeRates.size)
            assertEquals(2, sekExchangeRates.size)

            assertEquals(
                BigDecimal.valueOf(10.337),
                nokExchangeRates.filter { it.date.value == "2022-06-28" }[0].sdmxExchangeRateValue.value,
            )
            assertEquals(
                BigDecimal.valueOf(10.3065),
                nokExchangeRates.filter { it.date.value == "2022-06-29" }[0].sdmxExchangeRateValue.value,
            )

            assertEquals(
                BigDecimal.valueOf(10.6543),
                sekExchangeRates.filter { it.date.value == "2022-06-28" }[0].sdmxExchangeRateValue.value,
            )
            assertEquals(
                BigDecimal.valueOf(10.6848),
                sekExchangeRates.filter { it.date.value == "2022-06-29" }[0].sdmxExchangeRateValue.value,
            )
        }
    }

    @Nested
    inner class NorgesBankValutakursDataParsingTest {
        private val norgesBankXml =
            """
            <message:GenericData xmlns:footer="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message/footer"
                xmlns:generic="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/data/generic"
                xmlns:common="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/common"
                xmlns:message="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://www.sdmx.org/resources/sdmxml/schemas/v2_1/message https://registry.sdmx.org/schemas/v2_1/SDMXMessage.xsd">
                <message:Header>
                    <message:ID>IREF446885</message:ID>
                    <message:Test>false</message:Test>
                    <message:Prepared>2025-11-27T11:58:09Z</message:Prepared>
                    <message:Sender id="Unknown" />
                    <message:Receiver id="guest" />
                    <message:Structure structureID="NB_EXR_1_0" dimensionAtObservation="TIME_PERIOD">
                        <common:StructureUsage>
                            <Ref agencyID="NB" id="EXR" version="1.0" />
                        </common:StructureUsage>
                    </message:Structure>
                    <message:DataSetAction>Information</message:DataSetAction>
                    <message:DataSetID>023524f8-b8fc-4c82-a935-440ca12ae671</message:DataSetID>
                    <message:Extracted>2025-11-27T11:58:09</message:Extracted>
                    <message:ReportingBegin>2022-06-29T00:00:00</message:ReportingBegin>
                    <message:ReportingEnd>2022-06-29T23:59:59</message:ReportingEnd>
                </message:Header>
                <message:DataSet structureRef="NB_EXR_1_0">
                    <generic:Series>
                        <generic:SeriesKey>
                            <generic:Value id="FREQ" value="B" />
                            <generic:Value id="BASE_CUR" value="DKK" />
                            <generic:Value id="QUOTE_CUR" value="NOK" />
                            <generic:Value id="TENOR" value="SP" />
                        </generic:SeriesKey>
                        <generic:Attributes>
                            <generic:Value id="COLLECTION" value="C" />
                            <generic:Value id="CALCULATED" value="false" />
                            <generic:Value id="DECIMALS" value="2" />
                            <generic:Value id="UNIT_MULT" value="2" />
                        </generic:Attributes>
                        <generic:Obs>
                            <generic:ObsDimension value="2022-06-29" />
                            <generic:ObsValue value="138.54" />
                        </generic:Obs>
                    </generic:Series>
                </message:DataSet>
            </message:GenericData>
            """.trimIndent()

        @Test
        fun `Test at xml parses som forventet`() {
            // Arrange
            val mapper = SDMXValutakursRestKlientConfig().xmlMapper()

            // Act
            val norgesBankValutakursData = mapper.readValue(norgesBankXml, NorgesBankValutakursData::class.java)

            // Assert
            val valutakurs =
                norgesBankValutakursData.tilValutakurs(
                    valuta = "DKK",
                    frekvens = Frekvens.VIRKEDAG,
                    kursDato = LocalDate.of(2022, 6, 29),
                )

            assertEquals("DKK", valutakurs.valuta)
            assertEquals(LocalDate.of(2022, 6, 29), valutakurs.kursDato)
            assertEquals(BigDecimal.valueOf(1.3854), valutakurs.kurs)
        }
    }
}
