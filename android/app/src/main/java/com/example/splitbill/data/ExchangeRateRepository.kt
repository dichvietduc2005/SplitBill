package com.example.splitbill.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ExchangeRateResponse(
  val result: String = "",
  val base_code: String = "",
  val rates: Map<String, Double> = emptyMap()
)

class ExchangeRateRepository {
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun getExchangeRateToVnd(currency: String): Result<Double> {
    if (currency == "VND") return Result.success(1.0)
    return withContext(Dispatchers.IO) {
      try {
        val url = URL("https://open.er-api.com/v6/latest/$currency")
        val connection = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 8000
          readTimeout = 8000
          setRequestProperty("Accept", "application/json")
        }
        if (connection.responseCode in 200..299) {
          val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
          val parsed = json.decodeFromString<ExchangeRateResponse>(responseBody)
          val rate = parsed.rates["VND"] ?: return@withContext Result.failure(Exception("Không tìm thấy tỷ giá quy đổi sang VND"))
          Result.success(rate)
        } else {
          Result.failure(Exception("HTTP error ${connection.responseCode}"))
        }
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
  }
}
