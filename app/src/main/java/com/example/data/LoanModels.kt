package com.example.data

import kotlin.math.pow

// Data class representing a single month's payment in the amortization schedule
data class AmortizationPayment(
    val paymentNumber: Int,
    val emi: Double,
    val principalPaid: Double,
    val interestPaid: Double,
    val extraPrepaymentPaid: Double,
    val remainingBalance: Double
)

// One-time prepayment item
data class OneTimePrepayment(
    val id: String,
    val month: Int,
    val amount: Double
)

// Data class summarizing the loan and its payoff benefits
data class LoanSummary(
    val originalTotalInterest: Double,
    val originalTotalPayment: Double,
    val originalTenureMonths: Int,
    val originalMonthlyEmi: Double,
    
    val revisedTotalInterest: Double,
    val revisedTotalPayment: Double,
    val revisedTenureMonths: Int,
    
    val interestSaved: Double,
    val monthsSaved: Int,
    
    val originalSchedule: List<AmortizationPayment>,
    val revisedSchedule: List<AmortizationPayment>
)

object LoanCalculator {

    // Standard EMI formula: P * R * (1+R)^N / ((1+R)^N - 1)
    fun calculateMonthlyEmi(principal: Double, annualRate: Double, tenureMonths: Int): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        val r = (annualRate / 12.0) / 100.0
        if (r == 0.0) return principal / tenureMonths
        
        return principal * r * (1.0 + r).pow(tenureMonths) / ((1.0 + r).pow(tenureMonths) - 1.0)
    }

    // Comprehensive calculator that generates both original and revised schedules
    fun calculateLoan(
        principal: Double,
        annualRate: Double,
        tenureMonths: Int,
        monthlyPrepayment: Double = 0.0,
        yearlyPrepayment: Double = 0.0,
        oneTimePrepayments: List<OneTimePrepayment> = emptyList()
    ): LoanSummary {
        val originalEmi = calculateMonthlyEmi(principal, annualRate, tenureMonths)
        
        // 1. Calculate Original Schedule (no prepayments)
        val originalSchedule = mutableListOf<AmortizationPayment>()
        var originalBalance = principal
        val r = (annualRate / 12.0) / 100.0
        
        var originalTotalInterest = 0.0
        var m = 1
        
        while (originalBalance > 0.01 && m <= tenureMonths * 2) { // cap at 2x tenure to prevent infinite loop
            val interest = originalBalance * r
            var principalPaid = originalEmi - interest
            
            if (principalPaid >= originalBalance) {
                principalPaid = originalBalance
                originalBalance = 0.0
            } else {
                originalBalance -= principalPaid
            }
            
            originalTotalInterest += interest
            originalSchedule.add(
                AmortizationPayment(
                    paymentNumber = m,
                    emi = if (originalBalance == 0.0) principalPaid + interest else originalEmi,
                    principalPaid = principalPaid,
                    interestPaid = interest,
                    extraPrepaymentPaid = 0.0,
                    remainingBalance = originalBalance
                )
            )
            m++
        }
        val originalTotalPayment = principal + originalTotalInterest
        val originalFinalTenure = originalSchedule.size

        // Map of one-time prepayments by month
        val oneTimeMap = oneTimePrepayments.associate { it.month to it.amount }

        // 2. Calculate Revised Schedule (with prepayments)
        val revisedSchedule = mutableListOf<AmortizationPayment>()
        var revisedBalance = principal
        var revisedTotalInterest = 0.0
        var revMonth = 1
        
        while (revisedBalance > 0.01 && revMonth <= tenureMonths * 2) {
            val interest = revisedBalance * r
            var principalPaid = originalEmi - interest
            
            // Limit regular principal payment to remaining balance
            if (principalPaid >= revisedBalance) {
                principalPaid = revisedBalance
                revisedBalance = 0.0
                revisedTotalInterest += interest
                revisedSchedule.add(
                    AmortizationPayment(
                        paymentNumber = revMonth,
                        emi = principalPaid + interest,
                        principalPaid = principalPaid,
                        interestPaid = interest,
                        extraPrepaymentPaid = 0.0,
                        remainingBalance = 0.0
                    )
                )
                break
            }
            
            var balanceAfterEmi = revisedBalance - principalPaid
            
            // Apply prepayments if any
            val extraMonthly = monthlyPrepayment
            val extraYearly = if (revMonth % 12 == 0) yearlyPrepayment else 0.0
            val extraOneTime = oneTimeMap[revMonth] ?: 0.0
            
            var totalExtra = extraMonthly + extraYearly + extraOneTime
            var actualExtraPaid = 0.0
            
            if (totalExtra > 0.0) {
                if (totalExtra >= balanceAfterEmi) {
                    actualExtraPaid = balanceAfterEmi
                    revisedBalance = 0.0
                } else {
                    actualExtraPaid = totalExtra
                    revisedBalance = balanceAfterEmi - actualExtraPaid
                }
            } else {
                revisedBalance = balanceAfterEmi
            }
            
            revisedTotalInterest += interest
            revisedSchedule.add(
                AmortizationPayment(
                    paymentNumber = revMonth,
                    emi = originalEmi,
                    principalPaid = principalPaid,
                    interestPaid = interest,
                    extraPrepaymentPaid = actualExtraPaid,
                    remainingBalance = revisedBalance
                )
            )
            
            if (revisedBalance <= 0.01) {
                break
            }
            revMonth++
        }
        
        val revisedTotalPayment = principal + revisedTotalInterest
        val revisedFinalTenure = revisedSchedule.size
        
        val interestSaved = (originalTotalInterest - revisedTotalInterest).coerceAtLeast(0.0)
        val monthsSaved = (originalFinalTenure - revisedFinalTenure).coerceAtLeast(0)
        
        return LoanSummary(
            originalTotalInterest = originalTotalInterest,
            originalTotalPayment = originalTotalPayment,
            originalTenureMonths = originalFinalTenure,
            originalMonthlyEmi = originalEmi,
            revisedTotalInterest = revisedTotalInterest,
            revisedTotalPayment = revisedTotalPayment,
            revisedTenureMonths = revisedFinalTenure,
            interestSaved = interestSaved,
            monthsSaved = monthsSaved,
            originalSchedule = originalSchedule,
            revisedSchedule = revisedSchedule
        )
    }
}
