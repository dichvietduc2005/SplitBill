package com.example.splitbill.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExchangeRateRepositoryTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun testExchangeRateResponseParsing() {
    val sampleApiResponse = """
      {
        "result": "success",
        "provider": "https://www.exchangerate-api.com",
        "documentation": "https://www.exchangerate-api.com/docs/free",
        "terms_of_use": "https://www.exchangerate-api.com/terms",
        "time_last_update_unix": 1711238401,
        "time_last_update_utc": "Sun, 24 Mar 2024 00:00:01 +0000",
        "time_next_update_unix": 1711324801,
        "time_next_update_utc": "Mon, 25 Mar 2024 00:00:01 +0000",
        "time_eol_unix": 0,
        "base_code": "USD",
        "rates": {
          "USD": 1,
          "VND": 24850.5,
          "EUR": 0.924,
          "JPY": 151.2
        }
      }
    """.trimIndent()

    val parsed = json.decodeFromString<ExchangeRateResponse>(sampleApiResponse)

    assertEquals("success", parsed.result)
    assertEquals("USD", parsed.base_code)
    assertNotNull(parsed.rates["VND"])
    assertEquals(24850.5, parsed.rates["VND"]!!, 0.01)
  }
}
