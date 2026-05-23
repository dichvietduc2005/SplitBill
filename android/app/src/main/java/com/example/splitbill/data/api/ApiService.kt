package com.example.splitbill.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object ApiService {
  private const val BASE_URL = "http://192.168.1.206:8081" // Android emulator to localhost

  fun createClient(token: String? = null): HttpClient {
    return HttpClient(OkHttp) {
      install(ContentNegotiation) {
        json(Json {
          prettyPrint = true
          isLenient = true
          ignoreUnknownKeys = true
        })
      }
      install(Logging) {
        level = LogLevel.ALL
      }
      defaultRequest {
        url(BASE_URL)
        contentType(ContentType.Application.Json)
        if (token != null) {
          header(HttpHeaders.Authorization, "Bearer $token")
        }
      }
    }
  }
}
