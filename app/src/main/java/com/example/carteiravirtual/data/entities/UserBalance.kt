package com.example.carteiravirtual.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserBalance(
    @PrimaryKey val currency: String,
    val balance: Double
)