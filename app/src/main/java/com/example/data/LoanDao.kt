package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY timestamp DESC")
    fun getAllLoans(): Flow<List<LoanProfile>>

    @Query("SELECT * FROM loans WHERE id = :id LIMIT 1")
    suspend fun getLoanById(id: Int): LoanProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanProfile): Long

    @Update
    suspend fun updateLoan(loan: LoanProfile)

    @Delete
    suspend fun deleteLoan(loan: LoanProfile)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Int)
}
