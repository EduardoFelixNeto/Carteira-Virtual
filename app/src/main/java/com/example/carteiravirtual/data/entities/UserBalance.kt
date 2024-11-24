package com.example.carteiravirtual.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserBalance(
    @PrimaryKey val currency: String, // USD, BRL, EUR, BTC, ETH
    val balance: Double
)