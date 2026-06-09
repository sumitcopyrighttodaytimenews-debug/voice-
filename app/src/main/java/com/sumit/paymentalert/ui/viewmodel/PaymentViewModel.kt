package com.sumit.paymentalert.ui.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sumit.paymentalert.data.PaymentDatabase
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.sumit.paymentalert.data.RewardTransaction
import com.sumit.paymentalert.data.PaymentTransaction
import com.sumit.paymentalert.data.PreferencesHelper
import com.sumit.paymentalert.service.TTSManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PaymentDatabase.getDatabase(application)
    private val dao = db.paymentDao()
    private val rewardDao = db.rewardDao()
    private val prefs = PreferencesHelper(application)
    private val ttsManager = TTSManager(application)

    val originalTransactions: StateFlow<List<PaymentTransaction>> = dao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCoins: StateFlow<Int> = rewardDao.getTotalCoinsFlow()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val rewardsFlow = rewardDao.getAllRewardsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun claimReferralReward() {
        viewModelScope.launch {
            rewardDao.insertReward(
                RewardTransaction(
                    title = "Referral Bonus",
                    coinAmount = 50
                )
            )
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("all") // all, today, yesterday, month
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _upiId = MutableStateFlow(prefs.upiId)
    val upiId = _upiId.asStateFlow()

    private val _userName = MutableStateFlow(prefs.userName)
    val userName = _userName.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(prefs.ttsSpeed)
    val ttsSpeed = _ttsSpeed.asStateFlow()

    private val _ttsPitch = MutableStateFlow(prefs.ttsPitch)
    val ttsPitch = _ttsPitch.asStateFlow()

    private val _isChimeEnabled = MutableStateFlow(prefs.isChimeEnabled)
    val isChimeEnabled = _isChimeEnabled.asStateFlow()

    private val _ttsLanguage = MutableStateFlow(prefs.ttsLanguage)
    val ttsLanguage = _ttsLanguage.asStateFlow()

    val filteredTransactions: StateFlow<List<PaymentTransaction>> = combine(
        originalTransactions,
        searchQuery,
        selectedFilter
    ) { txs, query, filter ->
        txs.filter { tx ->
            val matchesQuery = query.isEmpty() || 
                    tx.sender.contains(query, ignoreCase = true) || 
                    tx.amount.toString().contains(query)
            
            val matchesDate = when (filter) {
                "today" -> isToday(tx.timestamp)
                "yesterday" -> isYesterday(tx.timestamp)
                "month" -> isThisMonth(tx.timestamp)
                else -> true
            }

            matchesQuery && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryTotal: StateFlow<Double> = combine(
        originalTransactions,
        selectedFilter
    ) { txs, filter ->
        txs.filter { tx ->
            when (filter) {
                "today" -> isToday(tx.timestamp)
                "yesterday" -> isYesterday(tx.timestamp)
                "month" -> isThisMonth(tx.timestamp)
                else -> true
            }
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("PaymentViewModel", "Live database updates triggered via broadcast signal")
        }
    }

    init {
        try {
            val filter = IntentFilter("com.sumit.paymentalert.NEW_PAYMENT")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.registerReceiver(refreshReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                application.registerReceiver(refreshReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("PaymentViewModel", "Error subscribing to refresh broadcast", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {}
        ttsManager.shutdown()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setUpiId(id: String) {
        prefs.upiId = id
        _upiId.value = id
    }

    fun setUserName(name: String) {
        prefs.userName = name
        _userName.value = name
    }

    fun setTtsSpeed(speed: Float) {
        prefs.ttsSpeed = speed
        _ttsSpeed.value = speed
        ttsManager.configureLanguage()
    }

    fun setTtsPitch(pitch: Float) {
        prefs.ttsPitch = pitch
        _ttsPitch.value = pitch
        ttsManager.configureLanguage()
    }

    fun setIsChimeEnabled(enabled: Boolean) {
        prefs.isChimeEnabled = enabled
        _isChimeEnabled.value = enabled
    }

    fun setTtsLanguage(lang: String) {
        prefs.ttsLanguage = lang
        _ttsLanguage.value = lang
        ttsManager.configureLanguage()
    }

    fun simulatePayment(amount: Double, senderName: String) {
        viewModelScope.launch {
            try {
                val transaction = PaymentTransaction(
                    amount = amount,
                    sender = senderName,
                    timestamp = System.currentTimeMillis()
                )
                dao.insertTransaction(transaction)

                // Store all data in Firebase Realtime Database
                com.sumit.paymentalert.data.FirebaseHelper.saveTransactionToFirebase(transaction)

                // Speak alert dynamically
                ttsManager.speakAlert(amount, senderName)
            } catch (e: Exception) {
                Log.e("PaymentViewModel", "Database write exception", e)
            }
        }
    }

    fun triggerVoiceTest() {
        ttsManager.testSpeak()
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            dao.deleteTransactionById(id)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            dao.clearAllTransactions()
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return yesterday.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisMonth(timestamp: Long): Boolean {
        val currentMonth = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        return currentMonth.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                currentMonth.get(Calendar.MONTH) == txDate.get(Calendar.MONTH)
    }
}
