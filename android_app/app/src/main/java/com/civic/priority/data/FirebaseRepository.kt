package com.civic.priority.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FirebaseRepository {
    private var db: FirebaseFirestore? = null
    private var issuesListener: ListenerRegistration? = null

    private val _issues = MutableStateFlow<List<Issue>>(emptyList())
    val issues: StateFlow<List<Issue>> = _issues

    init {
        try {
            db = Firebase.firestore
            setupListeners()
        } catch (e: Exception) {
            // Firebase not initialized yet (missing google-services.json)
            android.util.Log.e("FirebaseRepository", "Firebase not initialized. Using local mock data.", e)
        }
    }

    private fun setupListeners() {
        val firestore = db ?: return
        issuesListener = firestore.collection("issues")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.w("FirebaseRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // In a full implementation, you would map DocumentSnapshot to Issue data class here
                    // For now, this establishes the identical collection listener as the React website
                    android.util.Log.d("FirebaseRepository", "Received \${snapshot.size()} issues from Firestore")
                }
            }
    }

    fun cleanup() {
        issuesListener?.remove()
    }
}
