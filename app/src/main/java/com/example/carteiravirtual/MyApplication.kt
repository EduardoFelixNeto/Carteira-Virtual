package com.example.carteiravirtual

import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.carteiravirtual.data.database.AppDatabase

class MyApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        val workRequest = OneTimeWorkRequestBuilder<InitDatabaseWorker>().build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
