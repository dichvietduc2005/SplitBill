package com.example.splitbill.data

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRateResponse(
  val result: String,
  val base_code: String,
  val rates: Map<String, Double>
)

class ExchangeRateRepository {
  private val client = HttpClient {
    install(ContentNegotiation) {
      json(kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
      })
    }
  }

  suspend fun getExchangeRateToVnd(currency: String): Result<Double> {
    if (currency == "VND") return Result.success(1.0)
    return try {
      val response: ExchangeRateResponse = client.get("https://open.er-api.com/v6/latest/$currency").body()
      val rate = response.rates["VND"] ?: return Result.failure(Exception("Không tìm thấy tỷ giá quy đổi sang VND"))
      Result.success(rate)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
