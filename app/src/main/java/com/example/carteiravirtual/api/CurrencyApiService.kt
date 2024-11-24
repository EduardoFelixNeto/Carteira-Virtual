package com.example.carteiravirtual.api

import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {

    @GET("last/{moedas}")
    suspend fun getExchangeRates(@Path("moedas") moedas: String): Map<String, CurrencyResponse>
}
