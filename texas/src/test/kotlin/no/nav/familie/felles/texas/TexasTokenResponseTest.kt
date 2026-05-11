package no.nav.familie.felles.texas

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TexasTokenResponseTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `skal deserialisere token-respons fra JSON`() {
        val json =
            """
            {
              "access_token": "mitt-token-123",
              "expires_in": 3600,
              "token_type": "Bearer"
            }
            """.trimIndent()

        val response = objectMapper.readValue(json, TexasTokenResponse::class.java)

        assertEquals("mitt-token-123", response.accessToken)
        assertEquals(3600, response.utløperOm)
        assertEquals("Bearer", response.tokenType)
    }

    @Test
    fun `skal håndtere ulike token-verdier`() {
        val json =
            """
            {
              "access_token": "eyJhbGciOiJSUzI1NiJ9.payload.signature",
              "expires_in": 900,
              "token_type": "Bearer"
            }
            """.trimIndent()

        val response = objectMapper.readValue(json, TexasTokenResponse::class.java)

        assertEquals("eyJhbGciOiJSUzI1NiJ9.payload.signature", response.accessToken)
        assertEquals(900, response.utløperOm)
    }
}
