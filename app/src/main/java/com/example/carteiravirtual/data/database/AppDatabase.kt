package com.example.carteiravirtual.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.carteiravirtual.data.dao.UserBalanceDao
import com.example.carteiravirtual.data.entities.UserBalance

@Database(entities = [UserBalance::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userBalanceDao(): UserBalanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
