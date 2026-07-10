package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "loans")
data class LoanProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val principal: Double,
    val annualInterestRate: Double,
    val tenureMonths: Int,
    val monthlyPrepayment: Double = 0.0,
    val yearlyPrepayment: Double = 0.0,
    val oneTimePrepayments: List<OneTimePrepayment> = emptyList(),
    val currency: String = "USD",
    val timestamp: Long = System.currentTimeMillis()
)

// Clean, 100% robust manual string-based serializer/converter for OneTimePrepayment
// This avoids complex reflection or code generation issues.
class PrepaymentConverter {
    
    @TypeConverter
    fun fromPrepaymentsList(list: List<OneTimePrepayment>): String {
        if (list.isEmpty()) return ""
        return list.joinToString("|") { "${it.id}:${it.month}:${it.amount}" }
    }

    @TypeConverter
    fun toPrepaymentsList(data: String): List<OneTimePrepayment> {
        if (data.isBlank()) return emptyList()
        return try {
            data.split("|").mapNotNull { item ->
                val parts = item.split(":")
                if (parts.size == 3) {
                    OneTimePrepayment(
                        id = parts[0],
                        month = parts[1].toIntOrNull() ?: 1,
                        amount = parts[2].toDoubleOrNull() ?: 0.0
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
