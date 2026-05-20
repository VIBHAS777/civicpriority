package com.civic.priority.data

import androidx.compose.ui.graphics.Color
import java.util.UUID
import java.util.Date

// ─── Auth System ───

data class User(
    val id: String = UUID.randomUUID().toString(),
    var username: String,
    var email: String,
    var passwordHash: String = "",
    var role: UserRole,
    var joinDate: Long = System.currentTimeMillis()
)

enum class UserRole(val displayName: String) {
    COMMUNITY("Community Member"),
    ADMIN("Admin Authority"),
    SYSTEM_ADMIN("System Admin");

    companion object {
        fun fromDisplayName(name: String): UserRole {
            return entries.firstOrNull { it.displayName == name } ?: COMMUNITY
        }
    }
}

// ─── Location and Category Enums ───

enum class LocationZone(val displayName: String, val weight: Double) {
    HOSPITAL("Hospital Zone", 1.0),
    SCHOOL("School Zone", 0.8),
    RESIDENTIAL("Residential Area", 0.6),
    COMMERCIAL("Commercial District", 0.5),
    PARK("Public Park", 0.2);

    companion object {
        fun fromDisplayName(name: String): LocationZone {
            return entries.firstOrNull { it.displayName == name } ?: RESIDENTIAL
        }
    }
}

enum class IssueCategory(val displayName: String, val bonus: Double) {
    LIFT_MAINTENANCE("Lift Maintenance", 5.0),
    WATER_SUPPLY("Water Supply", 10.0),
    SANITATION("Sanitation", 5.0),
    ROADS("Roads & Infrastructure", 8.0),
    ELECTRICITY("Electrical & Power", 10.0),
    SECURITY("Security & Safety", 8.0),
    LANDSCAPING("Landscaping", 2.0),
    WASTE_MANAGEMENT("Waste Management", 2.0);

    companion object {
        fun fromDisplayName(name: String): IssueCategory {
            return entries.firstOrNull { it.displayName == name } ?: ROADS
        }
    }
}

enum class IssueStatus(val displayName: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    DEFERRED("Deferred"),
    RESOLVED("Resolved");

    companion object {
        fun fromDisplayName(name: String): IssueStatus {
            return entries.firstOrNull { it.displayName == name } ?: OPEN
        }
    }
}

enum class PriorityLevel(val displayName: String) {
    CRITICAL("Critical"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    fun getColor(): Color {
        return when (this) {
            CRITICAL -> Color.Red
            HIGH -> Color(0xFFFF9800)
            MEDIUM -> Color.Yellow
            LOW -> Color.Green
        }
    }
}

// ─── Comment ───

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val authorId: String,
    val authorName: String,
    val text: String,
    val date: Long = System.currentTimeMillis()
)

// ─── Audit Log ───

data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String,
    val issueTitle: String,
    val issueId: String,
    val oldStatus: String,
    val newStatus: String,
    val adminName: String,
    val note: String
)

// ─── Issue ───

data class Issue(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var description: String,
    var category: IssueCategory,
    var severity: Double, // 1-5
    var affectedPeople: Double, // 1-5000
    var locationType: LocationZone,
    var dateReported: Long = System.currentTimeMillis(),
    var imageData: ByteArray? = null,

    var reporterId: String,
    var reporterName: String,
    var votes: MutableSet<String> = mutableSetOf(),

    var status: IssueStatus = IssueStatus.OPEN,
    var deferralReason: String? = null,
    var isOverridden: Boolean = false,
    var comments: MutableList<Comment> = mutableListOf()
) {
    // Normalized Weighted Scoring Formula — identical to iOS
    val baseScore: Double
        get() {
            val normSeverity = (severity - 1.0) / 4.0
            val normPeople = minOf(affectedPeople / 5000.0, 1.0)
            val daysPending = maxOf(0.0, (System.currentTimeMillis() - dateReported) / 86400000.0)
            val normTime = minOf(Math.sqrt(daysPending) / Math.sqrt(365.0), 1.0)

            val weightedSum = (normSeverity * 0.35) + (normPeople * 0.25) + (normTime * 0.20) + (locationType.weight * 0.20)
            val scoreWithBonus = (weightedSum * 100.0) + category.bonus
            return minOf(scoreWithBonus, 92.0)
        }

    val voteScore: Double
        get() = minOf(votes.size * 0.5, 8.0)

    val finalScore: Double
        get() = minOf(baseScore + voteScore, 100.0)

    val priorityLevel: PriorityLevel
        get() {
            return when {
                finalScore >= 80 -> PriorityLevel.CRITICAL
                finalScore >= 50 -> PriorityLevel.HIGH
                finalScore >= 20 -> PriorityLevel.MEDIUM
                else -> PriorityLevel.LOW
            }
        }

    companion object {
        fun sort(lhs: Issue, rhs: Issue): Int {
            if (Math.abs(lhs.finalScore - rhs.finalScore) > 0.01) {
                return rhs.finalScore.compareTo(lhs.finalScore)
            }
            if (lhs.severity != rhs.severity) {
                return rhs.severity.compareTo(lhs.severity)
            }
            if (lhs.affectedPeople != rhs.affectedPeople) {
                return rhs.affectedPeople.compareTo(lhs.affectedPeople)
            }
            return lhs.dateReported.compareTo(rhs.dateReported)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Issue) return false
        return id == other.id &&
               status == other.status &&
               isOverridden == other.isOverridden &&
               votes == other.votes &&
               comments == other.comments
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + isOverridden.hashCode()
        result = 31 * result + votes.hashCode()
        result = 31 * result + comments.hashCode()
        return result
    }
}

// ─── Civic Resources ───

data class CivicResources(
    var fieldWorkers: Double = 10.0,
    var serviceVehicles: Double = 5.0,
    var workingHours: Double = 40.0
)
