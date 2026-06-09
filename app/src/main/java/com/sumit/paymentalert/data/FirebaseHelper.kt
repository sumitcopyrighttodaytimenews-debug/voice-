package com.sumit.paymentalert.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object FirebaseHelper {
    fun saveTransactionToFirebase(transaction: PaymentTransaction) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val mobile = currentUser?.email?.substringBefore("@") ?: "anonymous"
            
            val database = FirebaseDatabase.getInstance()
            // Store under "users/$mobile/transactions"
            val ref = database.getReference("users").child(mobile).child("transactions")
            
            val data = mapOf(
                "id" to transaction.id,
                "amount" to transaction.amount,
                "sender" to transaction.sender,
                "timestamp" to transaction.timestamp
            )
            
            // Push a new record under a unique key
            val newRef = ref.push()
            newRef.setValue(data).addOnSuccessListener {
                Log.d("FirebaseHelper", "Transaction saved successfully to Realtime Database: $data")
            }.addOnFailureListener { e ->
                Log.e("FirebaseHelper", "Failed to save transaction to Realtime Database", e)
            }
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Firebase database write error", e)
        }
    }
}
