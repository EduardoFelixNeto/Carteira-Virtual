package com.example.carteiravirtual.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.carteiravirtual.data.entities.UserBalance

@Dao
interface UserBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(balance: UserBalance): Long

    @Query("SELECT * FROM UserBalance WHERE currency = :currency")
    fun getBalance(currency: String): UserBalance? {
        return getBalanceOrNull(currency) ?: UserBalance(currency, 0.0)
    }

    @Query("SELECT * FROM UserBalance WHERE currency = :currency")
    fun getBalanceOrNull(currency: String): UserBalance?


    @Query("SELECT * FROM UserBalance")
    fun getAllBalances(): List<UserBalance>
}
