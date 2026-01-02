package no.nav.familie.restklient.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.jvm.java

data class TestData(
    val field: String,
)

class EmptyBean

class JsonMapperConfigTest {
    @Test
    fun `skal serialisere og deserialisere felt med privat synlighet`() {
        data class PrivateField(
            val visible: String,
            private val hidden: String = "secret",
        )
        val obj = PrivateField("yes")
        val json = jsonMapper.writeValueAsString(obj)
        val deserialized = jsonMapper.readValue(json, PrivateField::class.java)
        assertEquals(obj, deserialized)
    }

    @Test
    fun `skal støtte Kotlin data-klasser`() {
        val data = TestData("abc")
        val json = jsonMapper.writeValueAsString(data)
        val result = jsonMapper.readValue(json, TestData::class.java)
        assertEquals(data, result)
    }

    @Test
    fun `skal deserialisere YearMonth`() {
        val yearMonth = YearMonth.of(2024, 6)
        val json = "\"2024-06\""
        val result = jsonMapper.readValue(json, YearMonth::class.java)
        assertEquals(yearMonth, result)
    }

    @Test
    fun `skal deserialisere LocalDate`() {
        val date = LocalDate.of(2024, 6, 1)
        val json = "\"2024-06-01\""
        val result = jsonMapper.readValue(json, LocalDate::class.java)
        assertEquals(date, result)
    }

    @Test
    fun `skal deserialisere LocalDateTime`() {
        val dateTime = LocalDateTime.of(2024, 6, 1, 1, 2, 3, 400)
        val json = "\"2024-06-01T01:02:03.000000400\""
        val result = jsonMapper.readValue(json, LocalDateTime::class.java)
        assertEquals(dateTime, result)
    }

    @Test
    fun `skal deserialisere ZonedDateTime`() {
        val dateTime = ZonedDateTime.of(2024, 6, 1, 1, 2, 3, 400, ZoneId.of("Europe/Oslo"))
        val json = "\"2024-06-01T01:02:03.000000400+02:00\""
        val result = jsonMapper.readValue(json, ZonedDateTime::class.java)
        assertEquals(dateTime, result.toInstant().atZone(ZoneId.of("Europe/Oslo")))
    }

    @Test
    fun `skal deserialisere YearMonth over 9999`() {
        val yearMonth = YearMonth.of(999999, 6)
        val json = "\"999999-06\""
        val result = jsonMapper.readValue(json, YearMonth::class.java)
        assertEquals(yearMonth, result)
    }

    @Test
    fun `skal ignorere ukjente felter`() {
        val json = """{"field":"abc","unknown":"value"}"""
        val result = jsonMapper.readValue(json, TestData::class.java)
        assertEquals("abc", result.field)
    }

    @Test
    fun `skal ikke feile på tomme beans`() {
        val bean = EmptyBean()
        assertDoesNotThrow {
            jsonMapper.writeValueAsString(bean)
        }
    }

    @Test
    fun `skal skrive datoer som ISO-8601-strenger, ikke tidsstempler`() {
        val yearMonth = YearMonth.of(2024, 6)
        val json = jsonMapper.writeValueAsString(yearMonth)
        assertEquals("\"2024-06\"", json)
    }

    @Test
    fun `skal skrive støtte å skriver YearMonth over 9999`() {
        val yearMonth = YearMonth.of(999999999, 6)
        val json = jsonMapper.writeValueAsString(yearMonth)
        assertEquals("\"999999999-06\"", json)
    }

    @Test
    fun `skal skrive LocalDate`() {
        val date = LocalDateTime.of(2024, 6, 1, 1, 1).toLocalDate()
        val json = jsonMapper.writeValueAsString(date)
        assertEquals("\"2024-06-01\"", json)
    }

    @Test
    fun `skal skrive LocalDateTime`() {
        val dateTime = LocalDateTime.of(2024, 6, 1, 1, 1)
        val json = jsonMapper.writeValueAsString(dateTime)
        assertEquals("\"2024-06-01T01:01:00\"", json)
    }

    @Test
    fun `skal skrive ZonedDateTime`() {
        val dateTime = ZonedDateTime.of(2024, 6, 1, 1, 1, 42, 32, ZoneId.of("Europe/Oslo"))
        val json = jsonMapper.writeValueAsString(dateTime)
        assertEquals("\"2024-06-01T01:01:42.000000032+02:00\"", json)
    }
}
