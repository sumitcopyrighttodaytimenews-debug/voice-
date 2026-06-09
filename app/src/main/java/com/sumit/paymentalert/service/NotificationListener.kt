package com.sumit.paymentalert.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sumit.paymentalert.data.PaymentDatabase
import com.sumit.paymentalert.data.PaymentTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class NotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var ttsManager: TTSManager? = null

    override fun onCreate() {
        super.onCreate()
        ttsManager = TTSManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager?.shutdown()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.notification == null) return
        val extras = sbn.notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE, "")?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT, "")?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT, "")?.toString() ?: ""

        val fullText = "$title $text $subText"
        Log.d("NotificationListener", "Intercepted notification body: $fullText")

        // Search for dynamic payment phrases to identify received UPI monies
        val containsKeywords = fullText.contains("received", ignoreCase = true) ||
                fullText.contains("credited", ignoreCase = true) ||
                fullText.contains("sent", ignoreCase = true) ||
                fullText.contains("successfully paid", ignoreCase = true) ||
                fullText.contains("has sent you", ignoreCase = true) ||
                fullText.contains("प्राप्त", ignoreCase = true) ||
                fullText.contains("खाते में आया", ignoreCase = true)

        val containsCurrency = fullText.contains("₹") || 
                fullText.contains("rs", ignoreCase = true) || 
                fullText.contains("inr", ignoreCase = true)

        if (containsKeywords && containsCurrency) {
            parseAndAlert(fullText)
        }
    }

    private fun parseAndAlert(messageBody: String) {
        var amount: Double? = null
        var sender = "UPI Customer"

        // Regular expressions for clean and robust extraction
        val pattern1 = Pattern.compile("(.+?)\\s+has sent (?:₹|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
        val matcher1 = pattern1.matcher(messageBody)

        val pattern2 = Pattern.compile("(?:Received|Credited)\\s*(?:₹|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]+)?)\\s*(?:from|by)\\s+([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE)
        val matcher2 = pattern2.matcher(messageBody)

        val pattern3 = Pattern.compile("(?:₹|Rs\\.?)\\s*([0-9,]+(?:\\.[0-9]+)?)\\s+credited.*?from\\s+([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE)
        val matcher3 = pattern3.matcher(messageBody)

        val patternAmount = Pattern.compile("(?:₹|Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE)
        val matcherAmount = patternAmount.matcher(messageBody)

        if (matcher1.find()) {
            sender = matcher1.group(1)?.trim() ?: sender
            val amtStr = matcher1.group(2)?.replace(",", "") ?: ""
            amount = amtStr.toDoubleOrNull()
        } else if (matcher2.find()) {
            val amtStr = matcher2.group(1)?.replace(",", "") ?: ""
            amount = amtStr.toDoubleOrNull()
            sender = matcher2.group(2)?.trim() ?: sender
        } else if (matcher3.find()) {
            val amtStr = matcher3.group(1)?.replace(",", "") ?: ""
            amount = amtStr.toDoubleOrNull()
            sender = matcher3.group(2)?.trim() ?: sender
        } else if (matcherAmount.find()) {
            val amtStr = matcherAmount.group(1)?.replace(",", "") ?: ""
            amount = amtStr.toDoubleOrNull()
            
            val senderPattern = Pattern.compile("(?:from|by|by account of|sender)\\s+([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE)
            val senderMatcher = senderPattern.matcher(messageBody)
            if (senderMatcher.find()) {
                sender = senderMatcher.group(1)?.trim() ?: sender
            }
        }

        if (amount != null) {
            if (sender.lowercase().startsWith("money received")) {
                sender = sender.substring(14).trim()
            } else if (sender.lowercase().startsWith("payment received")) {
                sender = sender.substring(16).trim()
            } else if (sender.lowercase().startsWith("rs")) {
                sender = sender.substring(2).trim()
            }

            if (sender.isEmpty()) {
                sender = "UPI Client"
            }

            val finalAmount = amount
            val finalSender = sender

            serviceScope.launch {
                try {
                    val transaction = PaymentTransaction(
                        amount = finalAmount,
                        sender = finalSender,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    val db = PaymentDatabase.getDatabase(this@NotificationListener)
                    db.paymentDao().insertTransaction(transaction)

                    // Store all data in Firebase Realtime Database
                    com.sumit.paymentalert.data.FirebaseHelper.saveTransactionToFirebase(transaction)

                    // Sending broadcast to update UI instantly
                    val intent = Intent("com.sumit.paymentalert.NEW_PAYMENT")
                    sendBroadcast(intent)

                    // Speak payment confirmation via TTS Alert
                    ttsManager?.speakAlert(finalAmount, finalSender)
                } catch (e: Exception) {
                    Log.e("NotificationListener", "Database and TTS activation flow error", e)
                }
            }
        }
    }
}
