package com.civic.priority.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.civic.priority.data.*
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = UserDatabase.getInstance(application)

    var currentUser = mutableStateOf<User?>(null)
        private set
    var users = mutableStateListOf<User>()
        private set

    var issues = mutableStateListOf<Issue>()
        private set
    var availableResources = mutableStateOf(CivicResources(10.0, 5.0, 40.0))
        private set
    var logs = mutableStateListOf<String>()
        private set
    var auditLogs = mutableStateListOf<AuditLogEntry>()
        private set

    init {
        loadUsers()
        seedIssuesIfNeeded()
    }

    // ─── User Database Integration ───

    fun loadUsers() {
        users.clear()
        users.addAll(db.loadAll())
    }

    /** Authenticate with username + password */
    fun login(username: String, password: String): Pair<Boolean, String?> {
        val user = db.findUser(username)
            ?: return Pair(false, "User not found. Try registering.")

        if (!UserDatabase.verifyPassword(password, user.passwordHash)) {
            return Pair(false, "Incorrect password. Please try again.")
        }

        currentUser.value = user
        log("User $username logged in.")
        return Pair(true, null)
    }

    /** Register a new user with password — persists to database */
    fun register(username: String, password: String): Pair<Boolean, String?> {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return Pair(false, "Username cannot be empty.")
        if (trimmed.length < 3) return Pair(false, "Username must be at least 3 characters.")
        if (password.length < 4) return Pair(false, "Password must be at least 4 characters.")
        if (db.usernameExists(trimmed)) return Pair(false, "Username '$trimmed' is already taken.")

        val newUser = User(
            username = trimmed,
            email = "$trimmed@civic.com",
            passwordHash = UserDatabase.hashPassword(password),
            role = UserRole.COMMUNITY,
            joinDate = System.currentTimeMillis()
        )
        db.addUser(newUser)
        users.clear()
        users.addAll(db.loadAll())
        currentUser.value = newUser
        log("New user registered: $trimmed.")
        return Pair(true, null)
    }

    fun logout() {
        log("User ${currentUser.value?.username ?: ""} logged out.")
        currentUser.value = null
    }

    // ─── Issue Management ───

    private fun seedIssuesIfNeeded() {
        if (issues.isNotEmpty()) return
        log("Connecting to Firebase Firestore...")
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("issues").addSnapshotListener { snapshot, e ->
            log("Firestore snapshot received: error=${e?.message}, documents=${snapshot?.documents?.size}")
            if (e != null) {
                log("Listen failed: ${e.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val newIssues = snapshot.documents.mapNotNull { doc ->
                    try {
                        val firestoreIssue = doc.toObject(FirestoreIssue::class.java)
                        firestoreIssue?.copy(id = doc.id)?.toAppIssue()
                    } catch (e: Exception) {
                        log("Error parsing issue ${doc.id}: ${e.message}")
                        null
                    }
                }
                issues.clear()
                issues.addAll(newIssues.sortedWith(Comparator { a, b -> Issue.sort(a, b) }))
                log("Synced ${newIssues.size} issues from Firebase.")
            }
        }
    }

    fun addIssue(
        title: String,
        description: String,
        category: IssueCategory,
        severity: Double,
        affectedPeople: Double,
        locationType: LocationZone,
        imageData: ByteArray?
    ) {
        val user = currentUser.value ?: return
        val newIssue = Issue(
            title = title,
            description = description,
            category = category,
            severity = severity,
            affectedPeople = affectedPeople,
            locationType = locationType,
            dateReported = System.currentTimeMillis(),
            imageData = imageData,
            reporterId = user.id,
            reporterName = user.username
        )
        
        // Write to Firebase
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val docRef = firestore.collection("issues").document(newIssue.id)
        docRef.set(newIssue.toFirestoreIssue())
            .addOnSuccessListener { log("${user.username} reported: $title to Firebase") }
            .addOnFailureListener { e -> log("Error reporting issue: ${e.message}") }
    }

    fun toggleVote(issueId: String) {
        val user = currentUser.value ?: return
        val index = issues.indexOfFirst { it.id == issueId }
        if (index < 0) return

        val issue = issues[index]
        val newVotes = issue.votes.toMutableSet()
        if (newVotes.contains(user.id)) {
            newVotes.remove(user.id)
        } else {
            newVotes.add(user.id)
        }
        
        // Update Firebase
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("issues").document(issueId)
            .update("votes", newVotes.toList())
            .addOnFailureListener { e -> log("Error updating votes: ${e.message}") }
    }

    fun addComment(issueId: String, text: String) {
        val user = currentUser.value ?: return
        val index = issues.indexOfFirst { it.id == issueId }
        if (index < 0) return

        val issue = issues[index]
        val newComments = issue.comments.toMutableList()
        val comment = Comment(
            authorId = user.id,
            authorName = user.username,
            text = text,
            date = System.currentTimeMillis()
        )
        newComments.add(comment)
        
        // Update Firebase
        val firestoreIssueComments = newComments.map { 
            FirestoreComment(
                id = it.id,
                authorId = it.authorId,
                author = it.authorName,
                text = it.text,
                date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
            )
        }
        
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("issues").document(issueId)
            .update("comments", firestoreIssueComments)
            .addOnFailureListener { e -> log("Error adding comment: ${e.message}") }
    }

    fun overrideStatus(issueId: String, newStatus: IssueStatus, note: String) {
        val index = issues.indexOfFirst { it.id == issueId }
        if (index < 0) return

        val issue = issues[index]
        val user = currentUser.value ?: return

        // Update Firebase
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("issues").document(issueId)
            .update(
                mapOf(
                    "status" to newStatus.firestoreValue,
                    "overriddenBy" to user.id
                )
            )
            .addOnSuccessListener {
                log("Admin ${user.username} overrode status of '${issue.title}' to ${newStatus.displayName}")
            }
            .addOnFailureListener { e -> log("Error overriding status: ${e.message}") }
    }

    fun changeUserRole(userId: String, newRole: UserRole) {
        val index = users.indexOfFirst { it.id == userId }
        if (index < 0) return

        val user = users[index]
        val updatedUser = user.copy(role = newRole)
        users[index] = updatedUser

        if (currentUser.value?.id == userId) {
            currentUser.value = updatedUser
        }

        db.updateUser(updatedUser)
        log("System Admin changed role of ${user.username} to ${newRole.displayName}")
    }

    // ─── Resource Optimizer ───

    fun runOptimizer() {
        log("Running Category-Based Resource Optimizer...")
        log("Before optimization:")
        issues.forEach { log("  Issue: '${it.title}' | Status: ${it.status} | Overridden: ${it.isOverridden}") }

        var workers = availableResources.value.fieldWorkers
        var vehicles = availableResources.value.serviceVehicles
        var hours = availableResources.value.workingHours

        val openIssues = issues
            .filter { it.status == IssueStatus.OPEN || it.status == IssueStatus.DEFERRED }
            .sortedWith(Comparator { a, b -> Issue.sort(a, b) })

        for (issue in openIssues) {
            val index = issues.indexOfFirst { it.id == issue.id }
            if (index < 0) continue

            var requiredWorkers = 1.0
            var requiredVehicles = 0.0
            var requiredHours = 2.0

            when (issue.category) {
                IssueCategory.WATER_SUPPLY, IssueCategory.GENERATOR -> {
                    requiredWorkers = 3.0; requiredVehicles = 1.0; requiredHours = 8.0
                }
                IssueCategory.ELECTRICITY -> {
                    requiredWorkers = 2.0; requiredVehicles = 1.0; requiredHours = 4.0
                }
                IssueCategory.LIFT_MAINTENANCE -> {
                    requiredWorkers = 2.0; requiredVehicles = 0.0; requiredHours = 3.0
                }
                IssueCategory.HOUSEKEEPING, IssueCategory.COMMON_AREA -> {
                    requiredWorkers = 1.0; requiredVehicles = 1.0; requiredHours = 2.0
                }
                IssueCategory.SECURITY -> {
                    requiredWorkers = 2.0; requiredVehicles = 1.0; requiredHours = 4.0
                }
                IssueCategory.PARKING -> {
                    requiredWorkers = 1.0; requiredVehicles = 0.0; requiredHours = 4.0
                }
            }

            // Adjust based on severity
            if (issue.severity > 3.0) requiredWorkers += 1.0

            if (workers >= requiredWorkers && vehicles >= requiredVehicles && hours >= requiredHours) {
                workers -= requiredWorkers
                vehicles -= requiredVehicles
                hours -= requiredHours

                val updatedIssue = issue.copy(status = IssueStatus.IN_PROGRESS, deferralReason = null)
                log("ASSIGNING IN_PROGRESS: Index: $index, ID: ${issue.id}, Title: ${issue.title}")
                log("updatedIssue status: ${updatedIssue.status}")
                log("IdentityHash Before: ${System.identityHashCode(issues[index])} | Updated: ${System.identityHashCode(updatedIssue)}")
                issues[index] = updatedIssue
                log("IdentityHash After: ${System.identityHashCode(issues[index])} | Status After: ${issues[index].status}")
                log("Dispatched [${issue.category.displayName}] to ${issue.title}")
            } else {
                val reason = "Insufficient resources. Need ${requiredWorkers.toInt()}W, ${requiredVehicles.toInt()}V."
                val updatedIssue = issue.copy(status = IssueStatus.DEFERRED, deferralReason = reason)
                log("ASSIGNING DEFERRED: Index: $index, ID: ${issue.id}, Title: ${issue.title}")
                issues[index] = updatedIssue
            }
        }

        availableResources.value = CivicResources(workers, vehicles, hours)
        log("After optimization:")
        issues.forEach { log("  Issue: '${it.title}' | Status: ${it.status} | Overridden: ${it.isOverridden}") }
        log("Optimization complete.")
    }

    fun updateResources(workers: Double, vehicles: Double, hours: Double) {
        availableResources.value = CivicResources(workers, vehicles, hours)
    }

    fun log(message: String) {
        val formatter = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val timeString = formatter.format(Date())
        logs.add(0, "[$timeString] $message")
        android.util.Log.d("PriorityApp", "[$timeString] $message")
    }

    fun getIssueById(issueId: String): Issue? {
        return issues.firstOrNull { it.id == issueId }
    }
}
