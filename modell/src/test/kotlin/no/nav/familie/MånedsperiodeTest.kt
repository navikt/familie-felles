package no.nav.familie

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class MånedsperiodeTest {

    @Test
    fun `inneholder returnere true hvis måned er i perioden`() {
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))

        val inneholder = periode.inneholder(YearMonth.of(2019, 1))

        inneholder shouldBe true
    }

    @Test
    fun `inneholder returnere true hvis måned ikke er i perioden`() {
        val periode = Månedsperiode(YearMonth.of(2019, 2), YearMonth.of(2019, 5))

        val inneholder = periode.inneholder(YearMonth.of(2019, 1))

        inneholder shouldBe false
    }

    @Test
    fun `snitt returnerer lik periode for like perioder`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))

        val snitt = periode1.snitt(periode2)

        snitt shouldBe periode1
    }

    @Test
    fun `snitt returnerer null for periode uten overlap`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Månedsperiode(YearMonth.of(2018, 1), YearMonth.of(2018, 12))

        val snitt = periode1.snitt(periode2)

        snitt shouldBe null
    }

    @Test
    fun `snitt returnerer lik periode uansett hvilken periode som ligger til grunn`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 5))
        val periode2 = Månedsperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 12))

        val snitt1til2 = periode1.snitt(periode2)
        val snitt2til1 = periode2.snitt(periode1)

        snitt1til2 shouldBe snitt2til1
        snitt1til2 shouldBe Månedsperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 5))
    }

    @Test
    fun `inneholder returnerer true for periode som helt inneholder innsendt periode`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode2 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))

        val inneholder = periode1.inneholder(periode2)

        inneholder shouldBe true
    }

    @Test
    fun `inneholder returnerer false for periode som stikker utenfor innsendt periode`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode2 = Månedsperiode(YearMonth.of(2019, 2), YearMonth.of(2019, 4))

        val inneholder = periode1.inneholder(periode2)

        inneholder shouldBe false
    }

    @Test
    fun `omsluttesAv returnerer true for periode som helt omsluttes av innsendt periode`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        val periode2 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val inneholder = periode1.omsluttesAv(periode2)

        inneholder shouldBe true
    }

    @Test
    fun `omsluttesAv returnerer false for periode som nesten omsluttes av innsendt periode`() {
        val periode1 = Månedsperiode(YearMonth.of(2019, 2), YearMonth.of(2019, 4))
        val periode2 = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val inneholder = periode1.omsluttesAv(periode2)

        inneholder shouldBe false
    }

    @Test
    fun `overlapperIStartenAv returnerer true hvis denne perioden overlapper i starten av perioden som sendes inn`() {
        val periodeSomOverlapperStarten = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 1))
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperIStartenAv = periodeSomOverlapperStarten.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe true
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden er lik den som sendes inn`() {
        val periodeSomErLik = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperIStartenAv = periodeSomErLik.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe false
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden er før den som sendes inn`() {
        val periodeSomErFør = Månedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 12))
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperIStartenAv = periodeSomErFør.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe false
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden starter etter den som sendes inn`() {
        val periodeSomErInneI = Månedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))
        val periode = Månedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))

        val overlapperIStartenAv = periodeSomErInneI.overlapperKunIStartenAv(periode)

        overlapperIStartenAv shouldBe false
    }

    @Test
    fun `overlapperISluttenAv returnerer true hvis denne perioden overlapper i slutten av perioden som sendes inn`() {
        val periodeSomOverlapperSlutten = Månedsperiode(YearMonth.of(2019, 3), YearMonth.of(2019, 3))
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperISluttenAv = periodeSomOverlapperSlutten.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe true
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden er lik den som sendes inn`() {
        val periodeSomErLik = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperISluttenAv = periodeSomErLik.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe false
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden er etter den som sendes inn`() {
        val periodeSomErEtter = Månedsperiode(YearMonth.of(2019, 4), YearMonth.of(2019, 4))
        val periode = Månedsperiode(YearMonth.of(2019, 1), YearMonth.of(2019, 3))

        val overlapperISluttenAv = periodeSomErEtter.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe false
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden slutter før den som sendes inn`() {
        val periodeSomErInneI = Månedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))
        val periode = Månedsperiode(YearMonth.of(2018, 9), YearMonth.of(2018, 9))

        val overlapperISluttenAv = periodeSomErInneI.overlapperKunISluttenAv(periode)

        overlapperISluttenAv shouldBe false
    }

    @Test
    fun `lengdeIHeleMåneder returnerer korrekt antall måneder`() {
        val periode = Månedsperiode(YearMonth.of(2015, 9), YearMonth.of(2028, 3))

        val lengdeIHeleMåneder = periode.lengdeIHeleMåneder()

        lengdeIHeleMåneder shouldBe 151
    }
}
