package com.example.data

import kotlinx.coroutines.flow.Flow

class LoanRepository(private val loanDao: LoanDao) {
    val allLoans: Flow<List<LoanProfile>> = loanDao.getAllLoans()

    suspend fun getLoanById(id: Int): LoanProfile? {
        return loanDao.getLoanById(id)
    }

    suspend fun insertLoan(loan: LoanProfile): Long {
        return loanDao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: LoanProfile) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: LoanProfile) {
        loanDao.deleteLoan(loan)
    }

    suspend fun deleteLoanById(id: Int) {
        loanDao.deleteLoanById(id)
    }
}
