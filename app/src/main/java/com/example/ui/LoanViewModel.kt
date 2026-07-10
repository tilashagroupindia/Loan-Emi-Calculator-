package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LoanRepository

    // Raw input states
    val principalInput = MutableStateFlow("150000")
    val interestRateInput = MutableStateFlow("7.5")
    val tenureYearsInput = MutableStateFlow("15")
    val isTenureYears = MutableStateFlow(true) // toggle between years and months
    val tenureMonthsInput = MutableStateFlow("180")

    val monthlyPrepaymentInput = MutableStateFlow("0")
    val yearlyPrepaymentInput = MutableStateFlow("0")
    
    // Currency selection ("INR", "USD", "EUR")
    val selectedCurrency = MutableStateFlow("USD")
    
    // One-time custom prepayments list
    private val _oneTimePrepayments = MutableStateFlow<List<OneTimePrepayment>>(emptyList())
    val oneTimePrepayments: StateFlow<List<OneTimePrepayment>> = _oneTimePrepayments.asStateFlow()

    // Loaded loan profile (for tracking if we are editing an existing profile)
    val selectedProfileId = MutableStateFlow<Int?>(null)
    val profileNameInput = MutableStateFlow("")

    // List of saved loan profiles from Room
    val savedLoans: StateFlow<List<LoanProfile>>

    // Dynamic calculated loan summary derived from the current input states
    val loanSummary: StateFlow<LoanSummary>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LoanRepository(database.loanDao())
        
        savedLoans = repository.allLoans.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Reactive computation pipeline: re-calculate the loan whenever any core input changes
        loanSummary = combine(
            principalInput,
            interestRateInput,
            tenureYearsInput,
            tenureMonthsInput,
            isTenureYears,
            monthlyPrepaymentInput,
            yearlyPrepaymentInput,
            _oneTimePrepayments
        ) { inputs ->
            val principal = inputs[0].toString().toDoubleOrNull() ?: 0.0
            val rate = inputs[1].toString().toDoubleOrNull() ?: 0.0
            val years = inputs[2].toString().toIntOrNull() ?: 0
            val monthsVal = inputs[3].toString().toIntOrNull() ?: 0
            val isYears = inputs[4] as Boolean
            val monthlyPrep = inputs[5].toString().toDoubleOrNull() ?: 0.0
            val yearlyPrep = inputs[6].toString().toDoubleOrNull() ?: 0.0
            @Suppress("UNCHECKED_CAST")
            val customPreps = inputs[7] as List<OneTimePrepayment>

            val finalMonths = if (isYears) {
                years * 12
            } else {
                monthsVal
            }

            LoanCalculator.calculateLoan(
                principal = principal,
                annualRate = rate,
                tenureMonths = finalMonths,
                monthlyPrepayment = monthlyPrep,
                yearlyPrepayment = yearlyPrep,
                oneTimePrepayments = customPreps
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoanCalculator.calculateLoan(150000.0, 7.5, 180)
        )
    }

    // Helper functions for updating input states
    fun updatePrincipal(value: String) {
        principalInput.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateInterestRate(value: String) {
        interestRateInput.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateTenureYears(value: String) {
        tenureYearsInput.value = value.filter { it.isDigit() }
    }

    fun updateTenureMonths(value: String) {
        tenureMonthsInput.value = value.filter { it.isDigit() }
    }

    fun toggleTenureType() {
        isTenureYears.value = !isTenureYears.value
    }

    fun updateMonthlyPrepayment(value: String) {
        monthlyPrepaymentInput.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateYearlyPrepayment(value: String) {
        yearlyPrepaymentInput.value = value.filter { it.isDigit() || it == '.' }
    }

    fun updateCurrency(currency: String) {
        if (currency in listOf("INR", "USD", "EUR")) {
            selectedCurrency.value = currency
        }
    }

    // One-time prepayment management
    fun addOneTimePrepayment(month: Int, amount: Double) {
        if (month <= 0 || amount <= 0.0) return
        val currentList = _oneTimePrepayments.value.toMutableList()
        // Replace if exists for the same month, otherwise add
        currentList.removeAll { it.month == month }
        currentList.add(OneTimePrepayment(id = UUID.randomUUID().toString(), month = month, amount = amount))
        currentList.sortBy { it.month }
        _oneTimePrepayments.value = currentList
    }

    fun removeOneTimePrepayment(id: String) {
        val currentList = _oneTimePrepayments.value.toMutableList()
        currentList.removeAll { it.id == id }
        _oneTimePrepayments.value = currentList
    }

    fun clearOneTimePrepayments() {
        _oneTimePrepayments.value = emptyList()
    }

    // Room Database CRUD operations
    fun saveCurrentLoan(name: String) {
        if (name.isBlank()) return
        val principal = principalInput.value.toDoubleOrNull() ?: 0.0
        val rate = interestRateInput.value.toDoubleOrNull() ?: 0.0
        val isYears = isTenureYears.value
        val tenureMonths = if (isYears) {
            (tenureYearsInput.value.toIntOrNull() ?: 0) * 12
        } else {
            tenureMonthsInput.value.toIntOrNull() ?: 0
        }
        val monthlyPrep = monthlyPrepaymentInput.value.toDoubleOrNull() ?: 0.0
        val yearlyPrep = yearlyPrepaymentInput.value.toDoubleOrNull() ?: 0.0
        val customPreps = _oneTimePrepayments.value
        val currencyVal = selectedCurrency.value

        viewModelScope.launch {
            val profile = LoanProfile(
                id = selectedProfileId.value ?: 0,
                name = name,
                principal = principal,
                annualInterestRate = rate,
                tenureMonths = tenureMonths,
                monthlyPrepayment = monthlyPrep,
                yearlyPrepayment = yearlyPrep,
                oneTimePrepayments = customPreps,
                currency = currencyVal,
                timestamp = System.currentTimeMillis()
            )
            
            if (profile.id == 0) {
                repository.insertLoan(profile)
            } else {
                repository.updateLoan(profile)
            }
            
            // Clear current selection after saving to return to normal
            selectedProfileId.value = null
            profileNameInput.value = ""
        }
    }

    fun loadLoanProfile(profile: LoanProfile) {
        selectedProfileId.value = profile.id
        profileNameInput.value = profile.name
        principalInput.value = profile.principal.toString()
        interestRateInput.value = profile.annualInterestRate.toString()
        
        // Handle years vs months presentation
        if (profile.tenureMonths % 12 == 0) {
            isTenureYears.value = true
            tenureYearsInput.value = (profile.tenureMonths / 12).toString()
        } else {
            isTenureYears.value = false
            tenureMonthsInput.value = profile.tenureMonths.toString()
        }
        
        monthlyPrepaymentInput.value = profile.monthlyPrepayment.toString()
        yearlyPrepaymentInput.value = profile.yearlyPrepayment.toString()
        selectedCurrency.value = profile.currency
        _oneTimePrepayments.value = profile.oneTimePrepayments
    }

    fun deleteLoanProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteLoanById(id)
            if (selectedProfileId.value == id) {
                resetToDefaults()
            }
        }
    }

    fun resetToDefaults() {
        selectedProfileId.value = null
        profileNameInput.value = ""
        principalInput.value = "150000"
        interestRateInput.value = "7.5"
        tenureYearsInput.value = "15"
        isTenureYears.value = true
        tenureMonthsInput.value = "180"
        monthlyPrepaymentInput.value = "0"
        yearlyPrepaymentInput.value = "0"
        selectedCurrency.value = "USD"
        _oneTimePrepayments.value = emptyList()
    }
}
