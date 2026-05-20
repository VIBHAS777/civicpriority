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

        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val hourMs = 3600000L

        val reporterId = users.firstOrNull()?.id ?: UUID.randomUUID().toString()
        val reporterName = users.firstOrNull()?.username ?: "system"
        val voter1 = if (users.size > 1) users[1].id else UUID.randomUUID().toString()
        val voter2 = if (users.size > 2) users[2].id else UUID.randomUUID().toString()

        val issue1 = Issue(
            title = "Pothole on Main St",
            description = "Large pothole causing traffic issues.",
            category = IssueCategory.ROADS,
            severity = 3.0,
            affectedPeople = 50.0,
            locationType = LocationZone.COMMERCIAL,
            dateReported = now - 5 * dayMs,
            reporterId = reporterId,
            reporterName = reporterName,
            votes = mutableSetOf(reporterId, voter1)
        )

        val issue2 = Issue(
            title = "Water Main Break near Hospital",
            description = "Water leaking heavily.",
            category = IssueCategory.WATER_SUPPLY,
            severity = 5.0,
            affectedPeople = 500.0,
            locationType = LocationZone.HOSPITAL,
            dateReported = now - 2 * hourMs,
            reporterId = reporterId,
            reporterName = reporterName,
            votes = mutableSetOf(reporterId, voter2)
        )

        val issue3 = Issue(
            title = "Broken Park Bench",
            description = "Bench is broken in the central park.",
            category = IssueCategory.SANITATION,
            severity = 1.0,
            affectedPeople = 5.0,
            locationType = LocationZone.PARK,
            dateReported = now - 10 * dayMs,
            reporterId = reporterId,
            reporterName = reporterName,
            votes = mutableSetOf(reporterId)
        )

        // Add dummy comment
        if (users.size > 1) {
            issue1.comments.add(
                Comment(
                    authorId = users[1].id,
                    authorName = users[1].username,
                    text = "We will look into this tomorrow.",
                    date = now
                )
            )
        }

        issues.addAll(listOf(issue1, issue2, issue3))

        // Add dummy audit log
        if (users.size > 2) {
            auditLogs.add(
                AuditLogEntry(
                    timestamp = now - 5 * hourMs,
                    actionType = "Status Override",
                    issueTitle = "Broken Park Bench",
                    issueId = issue3.id.take(8),
                    oldStatus = "Open",
                    newStatus = "Deferred",
                    adminName = users[2].username,
                    note = "Awaiting replacement parts from vendor."
                )
            )
        }

        log("System initialized with seed data.")
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
        issues.add(newIssue)
        log("${user.username} reported: $title")
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
        issues[index] = issue.copy(votes = newVotes)
    }

    fun addComment(issueId: String, text: String) {
        val user = currentUser.value ?: return
        val index = issues.indexOfFirst { it.id == issueId }
        if (index < 0) return

        val issue = issues[index]
        val newComments = issue.comments.toMutableList()
        newComments.add(
            Comment(
                authorId = user.id,
                authorName = user.username,
                text = text,
                date = System.currentTimeMillis()
            )
        )
        issues[index] = issue.copy(comments = newComments)
    }

    fun overrideStatus(issueId: String, newStatus: IssueStatus, note: String) {
        val index = issues.indexOfFirst { it.id == issueId }
        if (index < 0) return

        val issue = issues[index]
        val oldStatus = issue.status.displayName
        issues[index] = issue.copy(
            status = newStatus,
            isOverridden = true
        )

        val user = currentUser.value
        if (user != null) {
            val entry = AuditLogEntry(
                timestamp = System.currentTimeMillis(),
                actionType = "Status Override",
                issueTitle = issue.title,
                issueId = issue.id.take(8),
                oldStatus = oldStatus,
                newStatus = newStatus.displayName,
                adminName = user.username,
                note = note
            )
            auditLogs.add(0, entry)
            log("Admin ${user.username} overrode status of '${issue.title}' to ${newStatus.displayName}")
        }
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
                IssueCategory.WATER_SUPPLY, IssueCategory.ROADS -> {
                    requiredWorkers = 3.0; requiredVehicles = 1.0; requiredHours = 8.0
                }
                IssueCategory.ELECTRICITY -> {
                    requiredWorkers = 2.0; requiredVehicles = 1.0; requiredHours = 4.0
                }
                IssueCategory.LIFT_MAINTENANCE -> {
                    requiredWorkers = 2.0; requiredVehicles = 0.0; requiredHours = 3.0
                }
                IssueCategory.SANITATION, IssueCategory.WASTE_MANAGEMENT -> {
                    requiredWorkers = 1.0; requiredVehicles = 1.0; requiredHours = 2.0
                }
                IssueCategory.SECURITY -> {
                    requiredWorkers = 2.0; requiredVehicles = 1.0; requiredHours = 4.0
                }
                IssueCategory.LANDSCAPING -> {
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
