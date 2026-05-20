package com.civic.priority.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.security.MessageDigest

/**
 * Lightweight persistent user database backed by SharedPreferences.
 * Mirrors iOS UserDatabase (which uses UserDefaults).
 */
class UserDatabase private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("CivicPriority_UserProfiles", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val storageKey = "user_profiles"

    init {
        if (loadAll().isEmpty()) {
            seedDefaults()
        }
    }

    // MARK: - Persistence

    fun saveAll(users: List<User>) {
        val json = gson.toJson(users)
        prefs.edit().putString(storageKey, json).apply()
    }

    fun loadAll(): List<User> {
        val json = prefs.getString(storageKey, null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // MARK: - CRUD

    fun addUser(user: User) {
        val all = loadAll().toMutableList()
        all.add(user)
        saveAll(all)
    }

    fun findUser(username: String): User? {
        return loadAll().firstOrNull { it.username.equals(username, ignoreCase = true) }
    }

    fun usernameExists(username: String): Boolean {
        return findUser(username) != null
    }

    fun updateUser(user: User) {
        val all = loadAll().toMutableList()
        val idx = all.indexOfFirst { it.id == user.id }
        if (idx >= 0) {
            all[idx] = user
            saveAll(all)
        }
    }

    fun deleteUser(id: String) {
        val all = loadAll().toMutableList()
        all.removeAll { it.id == id }
        saveAll(all)
    }

    fun resetToDefaults() {
        prefs.edit().remove(storageKey).apply()
        seedDefaults()
    }

    // MARK: - Seed Default Profiles

    private fun seedDefaults() {
        val now = System.currentTimeMillis()

        val defaultUsers = listOf(
            User(
                username = "john_doe",
                email = "john@civic.com",
                passwordHash = hashPassword("john123"),
                role = UserRole.COMMUNITY,
                joinDate = now
            ),
            User(
                username = "sarah_citizen",
                email = "sarah@civic.com",
                passwordHash = hashPassword("sarah123"),
                role = UserRole.COMMUNITY,
                joinDate = now
            ),
            User(
                username = "admin",
                email = "admin@civic.com",
                passwordHash = hashPassword("admin123"),
                role = UserRole.ADMIN,
                joinDate = now
            ),
            User(
                username = "sys_root",
                email = "root@civic.com",
                passwordHash = hashPassword("root123"),
                role = UserRole.SYSTEM_ADMIN,
                joinDate = now
            )
        )

        saveAll(defaultUsers)
    }

    companion object {
        @Volatile
        private var instance: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return instance ?: synchronized(this) {
                instance ?: UserDatabase(context.applicationContext).also { instance = it }
            }
        }

        /** SHA-256 hash for secure password storage */
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            return hashBytes.joinToString("") { "%02x".format(it) }
        }

        /** Verify a plaintext password against a stored hash */
        fun verifyPassword(password: String, hash: String): Boolean {
            return hashPassword(password) == hash
        }
    }
}
