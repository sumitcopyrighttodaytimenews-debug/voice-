package com.sumit.paymentalert.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {
    @Query("SELECT * FROM reward_transactions ORDER BY timestamp DESC")
    fun getAllRewardsFlow(): Flow<List<RewardTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(transaction: RewardTransaction): Long

    @Query("SELECT SUM(coinAmount) FROM reward_transactions")
    fun getTotalCoinsFlow(): Flow<Int?>
}
