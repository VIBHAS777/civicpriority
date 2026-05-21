package com.civic.priority.data

import androidx.compose.ui.graphics.Color
import java.util.Locale
import java.util.Date
import java.util.UUID

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
    BLOCK_A("Block A", 0.8),
    BLOCK_B("Block B", 0.8),
    BLOCK_C("Block C", 0.8),
    BASEMENT("Basement", 0.5),
    TERRACE("Terrace", 0.5),
    CLUBHOUSE("Clubhouse", 0.3),
    MAIN_GATE("Main Gate", 0.7),
    COMMON_AREA("Common Area", 0.4);

    companion object {
        fun fromDisplayName(name: String?): LocationZone {
            return entries.firstOrNull { it.displayName == name } ?: COMMON_AREA
        }
    }
}

enum class IssueCategory(val displayName: String, val bonus: Double) {
    WATER_SUPPLY("Water Supply", 15.0),
    SECURITY("Security", 14.0),
    ELECTRICITY("Electricity", 13.0),
    LIFT_MAINTENANCE("Lift Maintenance", 12.0),
    GENERATOR("Generator", 10.0),
    COMMON_AREA("Common Area", 6.0),
    PARKING("Parking", 5.0),
    HOUSEKEEPING("Housekeeping", 4.0);

    companion object {
        fun fromDisplayName(name: String?): IssueCategory {
            return entries.firstOrNull { it.displayName == name } ?: COMMON_AREA
        }
    }
}

enum class IssueStatus(val displayName: String, val firestoreValue: String) {
    OPEN("Open", "open"),
    IN_PROGRESS("In Progress", "inprogress"),
    DEFERRED("Deferred", "deferred"),
    RESOLVED("Resolved", "resolved");

    companion object {
        fun fromFirestore(name: String?): IssueStatus {
            return entries.firstOrNull { it.firestoreValue == name?.lowercase() } ?: OPEN
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

    fun toFirestoreIssue(): FirestoreIssue {
        return FirestoreIssue(
            id = id,
            title = title,
            category = category.displayName,
            description = description,
            severity = severity,
            affectedPeople = affectedPeople,
            locationType = locationType.displayName,
            status = status.firestoreValue,
            votes = votes.toList(),
            reporterId = reporterId,
            reporter = reporterName,
            createdAt = try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.format(Date(dateReported))
            } catch (e: Exception) { "" },
            overriddenBy = if (isOverridden) "admin" else null, // The web app uses admin user id, but "admin" is fine for flag
            comments = comments.map {
                FirestoreComment(
                    id = it.id,
                    authorId = it.authorId,
                    author = it.authorName,
                    text = it.text,
                    date = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.format(Date(it.date))
                    } catch (e: Exception) { "" }
                )
            },
            imageUrl = imageData?.let {
                "data:image/jpeg;base64," + android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
            }
        )
    }
}

// ─── Civic Resources ───

data class CivicResources(
    var fieldWorkers: Double = 10.0,
    var serviceVehicles: Double = 5.0,
    var workingHours: Double = 40.0
)

// ─── Firestore DTO ───

data class FirestoreComment(
    val id: Any? = null,
    val authorId: String = "",
    val author: String = "",
    val text: String = "",
    val date: String = ""
)

data class FirestoreIssue(
    val id: Any? = null,
    val title: String = "",
    val category: String = "",
    val description: String = "",
    val severity: Double = 3.0,
    val affectedPeople: Double = 10.0,
    val locationType: String = "",
    val status: String = "open",
    val votes: List<String> = emptyList(),
    val reporterId: String = "",
    val reporter: String = "",
    val createdAt: String = "",
    val overriddenBy: String? = null,
    val comments: List<FirestoreComment> = emptyList(),
    val imageUrl: String? = null
) {
    fun toAppIssue(): Issue {
        return Issue(
            id = id?.toString() ?: "",
            title = title,
            description = description,
            category = IssueCategory.fromDisplayName(category),
            severity = severity,
            affectedPeople = affectedPeople,
            locationType = LocationZone.fromDisplayName(locationType),
            imageData = try {
                if (imageUrl != null && imageUrl.startsWith("data:image")) {
                    val base64 = imageUrl.substringAfter(",")
                    android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                } else null
            } catch (e: Exception) { null },
            dateReported = try {
                if (createdAt.isNotEmpty()) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.parse(createdAt)?.time ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            },
            reporterId = reporterId,
            reporterName = reporter,
            votes = votes.toMutableSet(),
            status = IssueStatus.fromFirestore(status),
            isOverridden = overriddenBy != null,
            comments = comments.map {
                Comment(
                    id = it.id?.toString() ?: "",
                    authorId = it.authorId,
                    authorName = it.author,
                    text = it.text,
                    date = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.parse(it.date)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                )
            }.toMutableList()
        )
    }
}
