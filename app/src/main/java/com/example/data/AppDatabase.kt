package com.example.data

import android.content.Context
import androidx.room.*

@Database(entities = [LoanProfile::class], version = 2, exportSchema = false)
@TypeConverters(PrepaymentConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loan_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
