package com.example.carteiravirtual

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.carteiravirtual.data.database.AppDatabase
import com.example.carteiravirtual.data.entities.UserBalance

class InitDatabaseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val userBalanceDao = database.userBalanceDao()

        val defaultCurrencies = listOf("BRL", "USD", "EUR", "BTC", "ETH")
        defaultCurrencies.forEach { currency ->
            if (userBalanceDao.getBalanceOrNull(currency) == null) {
                userBalanceDao.insertOrUpdate(UserBalance(currency, 0.0))
            }
        }

        return Result.success()
    }
}
