package com.sumit.paymentalert.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amount: Double,
    val sender: String,
    val timestamp: Long = System.currentTimeMillis()
)
