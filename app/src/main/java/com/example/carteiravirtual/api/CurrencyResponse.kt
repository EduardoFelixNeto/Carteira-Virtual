package com.example.carteiravirtual.api

data class CurrencyResponse(
    val code: String,
    val codein: String,
    val name: String,
    val bid: String, // Taxa de compra (utilizaremos esta para conversão)
    val ask: String
)
