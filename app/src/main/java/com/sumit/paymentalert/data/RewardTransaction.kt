package com.sumit.paymentalert.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_transactions")
data class RewardTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val coinAmount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
